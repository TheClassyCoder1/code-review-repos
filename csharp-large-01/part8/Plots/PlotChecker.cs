using System.Diagnostics;
using System.Security.Cryptography;

namespace Lending.Part8.Plots;

public sealed record PlotParam(int? SizeV1);

public sealed record PlotMemo(string PoolPublicKeyOrPuzzleHash, string FarmerPublicKey, byte[] LocalMasterSk);

public interface IQuality
{
    string GetString();
}

public interface IV2Quality : IQuality
{
    byte[] GetPartialProof();
}

public interface IProver
{
    byte[] GetMemo();

    PlotParam GetParam();

    byte[] GetId();

    IReadOnlyList<IQuality> GetQualitiesForChallenge(byte[] challenge);
}

public interface IV1Prover : IProver
{
    byte[] GetFullProof(byte[] challenge, int index, bool parallelRead);
}

public interface IV2Prover : IProver
{
    int GetStrength();
}

public interface IProofEngine
{
    PlotMemo ParsePlotInfo(byte[] memo);

    byte[] MasterSkToLocalSk(byte[] localMasterSk);

    byte[] SolveProof(byte[] partialProof, byte[] plotId, int strength, int plotSize, bool testnet);

    string? ValidateProof(byte[] plotId, int size, byte[] challenge, byte[] fullProof);

    string? ValidateProofV2(byte[] plotId, int plotSize, byte[] challenge, int strength, byte[] fullProof, bool testnet);

    string EncodePuzzleHash(byte[] puzzleHash, string prefix);
}

public sealed record PlotInfo(IProver Prover, string? PoolPublicKey, byte[]? PoolContractPuzzleHash);

public sealed class ProofDroppedException() : Exception("GRResult_NoProof received");

public sealed class PlotChecker(IProofEngine engine, ILogger<PlotChecker> log)
{
    private const int PlotSizeV2 = 28;

    private readonly object _totalsLock = new();
    private readonly Dictionary<int, int> _totalGoodPlotsV1 = new();
    private readonly List<string> _badPlotsList = new();
    private int _totalGoodPlotsV2;
    private long _totalSize;

    public string AddressPrefix { get; init; } = "xch";
    public bool ParallelRead { get; init; } = true;
    public bool Testnet { get; init; }
    public int Challenges { get; init; } = 30;

    public void ProcessPlot(string plotPath, PlotInfo plotInfo, int numStart, int numEnd, object @lock)
    {
        var pr = plotInfo.Prover;

        // Look up local_sk from plot to save locked memory
        var memo = engine.ParsePlotInfo(pr.GetMemo());
        var farmerPublicKey = memo.FarmerPublicKey;
        var localSk = Convert.ToHexString(engine.MasterSkToLocalSk(memo.LocalMasterSk));

        lock (@lock)
        {
            log.LogInformation($"Testing plot {plotPath} k={pr.GetParam()}");
            if (plotInfo.PoolPublicKey is not null)
            {
                log.LogInformation($"\t{"Pool public key:",-23} {plotInfo.PoolPublicKey}");
            }
            if (plotInfo.PoolContractPuzzleHash is not null)
            {
                var pca = engine.EncodePuzzleHash(plotInfo.PoolContractPuzzleHash, AddressPrefix);
                log.LogInformation($"\t{"Pool contract address:",-23} {pca}");
            }
            log.LogInformation($"\t{"Farmer public key:",-23} {farmerPublicKey}");
            log.LogInformation($"\t{"Local sk:",-23} {localSk}");
        }

        var totalProofs = 0;
        var caughtException = false;
        for (var i = numStart; i < numEnd; i++)
        {
            var challenge = SHA256.HashData(ChallengeBytes(i));
            IReadOnlyList<IQuality> qualities;
            // Some plot errors cause GetQualitiesForChallenge to throw
            try
            {
                var qualityTimer = Stopwatch.StartNew();
                qualities = pr.GetQualitiesForChallenge(challenge);
                var qualitySpentTime = qualityTimer.ElapsedMilliseconds;
                if (qualitySpentTime > 8000)
                {
                    log.LogWarning(
                        $"\tLooking up qualities took: {qualitySpentTime} ms. This should be below 8 seconds " +
                        $"to minimize risk of losing rewards. Filepath: {plotPath}");
                }
                else
                {
                    log.LogInformation($"\tLooking up qualities took: {qualitySpentTime} ms. Filepath: {plotPath}");
                }
            }
            catch (OperationCanceledException)
            {
                log.LogWarning("Interrupted, closing");
                return;
            }
            catch (ProofDroppedException)
            {
                log.LogInformation($"Proof dropped due to line point compression. Filepath: {plotPath}");
                continue;
            }
            catch (InvalidOperationException e)
            {
                log.LogError($"{e.GetType()}: {e.Message} error in getting challenge qualities for plot {plotPath}");
                caughtException = true;
                continue;
            }
            catch (Exception e)
            {
                log.LogError($"{e.GetType()}: {e.Message} error in getting challenge qualities for plot {plotPath}");
                caughtException = true;
                break;
            }

            for (var index = 0; index < qualities.Count; index++)
            {
                var quality = qualities[index];
                // Other plot errors cause GetFullProof or ValidateProof to throw
                try
                {
                    var proofTimer = Stopwatch.StartNew();
                    var qualityStr = quality.GetString();

                    byte[] fullProof;
                    if (pr is IV1Prover v1)
                    {
                        fullProof = v1.GetFullProof(challenge, index, ParallelRead);
                    }
                    else if (pr is IV2Prover v2 && quality is IV2Quality v2Quality)
                    {
                        fullProof = engine.SolveProof(v2Quality.GetPartialProof(), v2.GetId(), v2.GetStrength(), PlotSizeV2, Testnet);
                    }
                    else
                    {
                        throw new InvalidOperationException("Unsupported prover/quality combination");
                    }

                    var proofSpentTime = proofTimer.ElapsedMilliseconds;
                    if (proofSpentTime > 15000)
                    {
                        var action = pr is IV1Prover ? "Finding" : "Solving";
                        log.LogWarning(
                            $"\t{action} proof took: {proofSpentTime} ms. This should be below 15 seconds " +
                            $"to minimize risk of losing rewards. Filepath: {plotPath}");
                    }
                    else
                    {
                        log.LogInformation($"\tFinding proof took: {proofSpentTime} ms. Filepath: {plotPath}");
                    }

                    var verQualityStr = pr is IV2Prover v2b
                        ? engine.ValidateProofV2(pr.GetId(), PlotSizeV2, challenge, v2b.GetStrength(), fullProof, Testnet)
                        : engine.ValidateProof(pr.GetId(), pr.GetParam().SizeV1 ?? 0, challenge, fullProof);

                    if (qualityStr == verQualityStr)
                    {
                        totalProofs++;
                    }
                    else
                    {
                        log.LogWarning(
                            $"\tQuality doesn't match with proof. Filepath: {plotPath} " +
                            "This can occasionally happen with a compressed plot.");
                    }
                }
                catch (ProofDroppedException)
                {
                    log.LogInformation($"Proof dropped due to line point compression. Filepath: {plotPath}");
                }
                catch (InvalidOperationException e)
                {
                    log.LogError($"{e.GetType()}: {e.Message} error in getting full proof for plot {plotPath}");
                    caughtException = true;
                }
                catch (Exception e)
                {
                    log.LogError(
                        $"{e.GetType()}: {e.Message} error in proving/verifying for plot {plotPath}. Filepath: {plotPath}");
                    caughtException = true;
                }
            }
            if (caughtException)
            {
                break;
            }
        }

        var ratio = Math.Round(totalProofs / (double)Challenges, 4);
        if (totalProofs > 0 && !caughtException)
        {
            log.LogInformation($"\tProofs {totalProofs} / {Challenges}, {ratio}. Filepath: {plotPath}");
            var param = pr.GetParam();
            lock (_totalsLock)
            {
                if (param.SizeV1 is { } k)
                {
                    _totalGoodPlotsV1[k] = _totalGoodPlotsV1.GetValueOrDefault(k) + 1;
                }
                else
                {
                    _totalGoodPlotsV2++;
                }
                _totalSize += new FileInfo(plotPath).Length;
            }
        }
        else
        {
            log.LogError($"\tProofs {totalProofs} / {Challenges}, {ratio} Filepath: {plotPath}");
            lock (_totalsLock)
            {
                _badPlotsList.Add(plotPath);
            }
        }
    }

    private static byte[] ChallengeBytes(int i)
    {
        var bytes = new byte[32];
        var value = BitConverter.GetBytes(i);
        if (BitConverter.IsLittleEndian)
        {
            Array.Reverse(value);
        }
        value.CopyTo(bytes, 32 - value.Length);
        return bytes;
    }
}
