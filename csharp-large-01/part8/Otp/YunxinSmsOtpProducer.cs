using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace Lending.Part8.Otp;

public sealed record UserInfo(string Id, string? Mobile);

public enum OtpTypes
{
    Sms,
    Email,
}

public interface IOtpTokenStore
{
    void Store(UserInfo userInfo, string token, string receiver, OtpTypes type);
}

public sealed class YunxinSms
{
    public int Code { get; set; }
    public string? Msg { get; set; }
    public string? Obj { get; set; }
}

public static class SmsOtpAuthnYunxinCheckSumBuilder
{
    public static string GetCheckSum(string appSecret, string nonce, string curTime) =>
        Convert.ToHexString(SHA1.HashData(Encoding.UTF8.GetBytes(appSecret + nonce + curTime))).ToLowerInvariant();
}

public sealed class StringGenerator(string alphabet, int length)
{
    public const string DefaultCodeNumber = "0123456789";

    public string RandomGenerate() =>
        new(Enumerable.Range(0, length).Select(_ => alphabet[RandomNumberGenerator.GetInt32(alphabet.Length)]).ToArray());
}

public sealed class YunxinSmsOtpProducer(
    IHttpClientFactory httpClientFactory,
    IOtpTokenStore optTokenStore,
    IConfiguration configuration,
    ILogger<YunxinSmsOtpProducer> logger)
{
    private const string ServerUrl = "https://api.netease.im/sms/sendcode.action";
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNameCaseInsensitive = true };

    private readonly string _appKey = configuration["Otp:Yunxin:AppKey"] ?? string.Empty;
    private readonly string _appSecret = configuration["Otp:Yunxin:AppSecret"] ?? string.Empty;
    private readonly string _templateId = configuration["Otp:Yunxin:TemplateId"] ?? string.Empty;
    private readonly int _digits = configuration.GetValue("Otp:Digits", 6);

    public async Task<bool> ProduceAsync(UserInfo userInfo, CancellationToken ct = default)
    {
        // 手机号
        var mobile = userInfo.Mobile;
        if (!string.IsNullOrEmpty(mobile))
        {
            try
            {
                using var httpPost = new HttpRequestMessage(HttpMethod.Post, ServerUrl);
                var curTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString();
                // 随机数
                var nonce = new StringGenerator(StringGenerator.DefaultCodeNumber, 6).RandomGenerate();
                var checkSum = SmsOtpAuthnYunxinCheckSumBuilder.GetCheckSum(_appSecret, nonce, curTime);
                logger.LogDebug("AppKey " + _appKey + " ,Nonce " + nonce + ", CurTime " + curTime + " ,checkSum " + checkSum);
                // 设置请求的header
                httpPost.Headers.Add("AppKey", _appKey);
                httpPost.Headers.Add("Nonce", nonce);
                httpPost.Headers.Add("CurTime", curTime);
                httpPost.Headers.Add("CheckSum", checkSum);

                // 设置请求的的参数，requestBody参数
                httpPost.Content = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["templateid"] = _templateId,
                    ["mobile"] = userInfo.Mobile!,
                    ["codeLen"] = _digits.ToString(),
                });

                // 执行请求
                using var response = await httpClientFactory.CreateClient(nameof(YunxinSmsOtpProducer)).SendAsync(httpPost, ct);
                //{"code":200,"msg":"1","obj":"740673"}
                var responseString = await response.Content.ReadAsStringAsync(ct);
                logger.LogDebug("responseString " + responseString);
                var yunxinSms = JsonSerializer.Deserialize<YunxinSms>(responseString, JsonOptions)!;
                logger.LogDebug("responseEntity code " + yunxinSms.Obj);
                nonce = yunxinSms.Obj ?? nonce;
                logger.LogDebug("nonce " + nonce);
                optTokenStore.Store(userInfo, nonce, userInfo.Mobile!, OtpTypes.Sms);
                return true;
            }
            catch (Exception e)
            {
                logger.LogError(e, " produce code error ");
            }
        }
        return false;
    }
}
