using System.Threading.Channels;
using Lending.Servicing.Part4.Domain;
using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.Disbursement;

public sealed record DisbursementSubmission(
    DisbursementRequest Request,
    TaskCompletionSource<ScheduleDecision> Completion);

/// <summary>
/// Accepts disbursement requests from any number of producers and feeds them
/// to <see cref="DisbursementScheduler"/> one at a time from a single consumer
/// loop. This is what keeps the scheduler's daily-limit bookkeeping consistent.
/// </summary>
public sealed class DisbursementPump : IAsyncDisposable
{
    private readonly Channel<DisbursementSubmission> _channel;
    private readonly DisbursementScheduler _scheduler;
    private readonly IClock _clock;
    private readonly IServicingLog _log;
    private readonly CancellationTokenSource _stopping = new();
    private readonly Task _consumer;

    public DisbursementPump(DisbursementScheduler scheduler, IClock clock, IServicingLog log, int capacity = 1_000)
    {
        _scheduler = scheduler;
        _clock = clock;
        _log = log;
        _channel = Channel.CreateBounded<DisbursementSubmission>(new BoundedChannelOptions(capacity)
        {
            SingleReader = true,
            SingleWriter = false,
            FullMode = BoundedChannelFullMode.Wait,
        });
        _consumer = Task.Run(() => ConsumeAsync(_stopping.Token));
    }

    public async Task<ScheduleDecision> SubmitAsync(DisbursementRequest request, CancellationToken cancellationToken)
    {
        var errors = DisbursementValidator.Validate(request, DateOnly.FromDateTime(_clock.UtcNow.UtcDateTime));
        if (errors.Count > 0)
        {
            throw new ArgumentException("Disbursement request is invalid: " + string.Join("; ", errors), nameof(request));
        }

        var completion = new TaskCompletionSource<ScheduleDecision>(TaskCreationOptions.RunContinuationsAsynchronously);
        await _channel.Writer.WriteAsync(new DisbursementSubmission(request, completion), cancellationToken).ConfigureAwait(false);
        return await completion.Task.WaitAsync(cancellationToken).ConfigureAwait(false);
    }

    private async Task ConsumeAsync(CancellationToken stoppingToken)
    {
        await foreach (var submission in _channel.Reader.ReadAllAsync(stoppingToken).ConfigureAwait(false))
        {
            try
            {
                var earliest = BusinessCalendar.NextBusinessDayOnOrAfter(submission.Request.RequestedDate);
                var decision = _scheduler.Schedule(submission.Request, earliest);
                _log.Info("Disbursement {0} for loan {1}: {2}", submission.Request.RequestId, submission.Request.LoanId, decision);
                submission.Completion.TrySetResult(decision);
            }
            catch (Exception ex)
            {
                _log.Error(ex, "Scheduling disbursement {0} failed", submission.Request.RequestId);
                submission.Completion.TrySetException(ex);
            }
        }
    }

    public async ValueTask DisposeAsync()
    {
        _channel.Writer.TryComplete();
        try
        {
            await _consumer.ConfigureAwait(false);
        }
        catch (OperationCanceledException)
        {
            // Shutdown requested while draining.
        }
        finally
        {
            _stopping.Cancel();
            _stopping.Dispose();
        }
    }
}
