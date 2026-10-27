using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace Lending.Part8.Jobs;

public sealed record SnapshotJobInfo(string Database, string Table);

public sealed record JobInfo(string State, IReadOnlyList<string> SegmentIds, SnapshotJobInfo? SnapshotJobInfo);

public sealed class JobNotifierSettings
{
    public string? JobFinishedNotifierUrl { get; set; }
    public string? JobFinishedNotifierUsername { get; set; }
    public string? JobFinishedNotifierPassword { get; set; }
}

public static class JobFinishedNotifier
{
    private static string? Url;

    public static async Task PostJobInfoAsync(JobInfo info, JobNotifierSettings config, ILogger log)
    {
        Url = config.JobFinishedNotifierUrl;
        log.LogInformation("post job info parameter, url : {Url}, state : {State}, segmentId : {SegmentIds}, table : {Table}",
            Url, info.State, info.SegmentIds,
            info.SnapshotJobInfo != null
                ? info.SnapshotJobInfo.Database + "." + info.SnapshotJobInfo.Table
                : null);
        if (Url == null || "READY".Equals(info.State, StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        try
        {
            using var handler = new HttpClientHandler
            {
                ServerCertificateCustomValidationCallback = HttpClientHandler.DangerousAcceptAnyServerCertificateValidator,
            };
            using var httpClient = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(3) };
            using var httpPost = new HttpRequestMessage(HttpMethod.Post, Url);
            var username = config.JobFinishedNotifierUsername;
            var password = config.JobFinishedNotifierPassword;
            if (username != null && password != null)
            {
                log.LogInformation("use basic auth.");
                httpPost.Headers.Authorization = MakeToken(username, password);
            }
            httpPost.Content = new StringContent(JsonSerializer.Serialize(info), Encoding.UTF8, "application/json");
            using var response = await httpClient.SendAsync(httpPost);
            var code = (int)response.StatusCode;
            if (response.StatusCode == HttpStatusCode.OK)
            {
                log.LogInformation("Post job info to " + Url + " successful.");
            }
            else
            {
                log.LogInformation("Post job info to " + Url + " failed. Status code: " + code);
            }
        }
        catch (Exception e) when (e is HttpRequestException or TaskCanceledException)
        {
            log.LogWarning(e, "Error occurred when post job status.");
        }
    }

    private static AuthenticationHeaderValue MakeToken(string username, string password) =>
        new("Basic", Convert.ToBase64String(Encoding.UTF8.GetBytes(username + ":" + password)));
}
