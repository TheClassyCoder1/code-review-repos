using System.Text.Encodings.Web;
using Lending.Part2.Account;
using Lending.Part2.Archive;
using Lending.Part2.Autopay;
using Lending.Part2.Branches;
using Lending.Part2.Bureau;
using Lending.Part2.Collections;
using Lending.Part2.ESign;
using Lending.Part2.Exports;
using Lending.Part2.Links;
using Lending.Part2.Partners;
using Lending.Part2.Pricing;
using Lending.Part2.Reporting;
using Lending.Part2.Security;
using Lending.Part2.Statements;
using Lending.Part2.Uploads;
using Lending.Part2.Webhooks;
using Microsoft.Extensions.Options;

namespace Lending.Part2;

public static class ServicingPart2ServiceCollectionExtensions
{
    public static IServiceCollection AddServicingPart2(this IServiceCollection services, IConfiguration configuration, IWebHostEnvironment environment)
    {
        services.AddSingleton(HtmlEncoder.Default);
        services.AddSingleton<IStatementRepository, InMemoryStatementRepository>();
        services.AddSingleton<StatementTextEncoder>().AddSingleton<StatementHtmlRenderer>().AddScoped<StatementService>();
        services.AddSingleton<ExportRecipientRegistry>().AddSingleton<GpgFileEncryptor>().AddScoped<PartnerExportService>();

        services.AddSingleton<CallbackUrlPolicy>().AddSingleton<WebhookDeliveryJournal>().AddScoped<WebhookSubscriptionService>();
        services.AddSingleton<IWebhookSubscriptionStore, InMemoryWebhookSubscriptionStore>();
        services.AddHttpClient<WebhookDispatcher>(client => client.Timeout = TimeSpan.FromSeconds(10));

        services.AddOptions<BureauOptions>().Bind(configuration.GetSection(BureauOptions.SectionName)).ValidateOnStart();
        services.AddSingleton<IValidateOptions<BureauOptions>>(new BureauOptionsValidator(environment));
        services.AddHttpClient<BureauClient>((sp, client) =>
            {
                client.BaseAddress = new Uri(sp.GetRequiredService<IOptions<BureauOptions>>().Value.BaseAddress);
                client.Timeout = TimeSpan.FromSeconds(15);
            })
            .ConfigurePrimaryHttpMessageHandler(sp => BureauClient.CreateHandler(sp.GetRequiredService<IOptions<BureauOptions>>().Value));

        services.AddSingleton<CollectionsAgencyClient>().AddScoped<CollectionsPlacementSync>();
        services.AddSingleton<StagedUploadStore>().AddSingleton<MalwareScanner>();
        services.AddSingleton<ClientSecretGenerator>().AddSingleton<PartnerCredentialStore>();
        services.AddSingleton<PortalClientRegistry>().AddScoped<RegisteredRedirectFilter>();
        services.AddSingleton<ArchiveSealer>().AddSingleton<ArchiveBlobStore>().AddSingleton<ArchivePayloadSerializer>().AddScoped<ArchiveReplayService>();
        services.AddSingleton<ESignSessionStore>().AddSingleton<ESignSessionFactory>().AddSingleton<ESignCompletionPage>();
        services.AddSingleton<IPaymentLinkStore, InMemoryPaymentLinkStore>().AddSingleton<PaymentLinkIssuer>();
        services.AddSingleton<BranchCalendar>().AddSingleton<RateCardService>();
        services.AddSingleton<ReportSortCatalog>().AddSingleton<DelinquencyReportRepository>().AddScoped<DelinquencyReportService>();

        services.AddSingleton<ILoanDirectory, InMemoryLoanDirectory>();
        services.AddSingleton<IBalanceStore, InMemoryBalanceStore>();
        services.AddSingleton<IAutopayScheduleStore, InMemoryAutopayScheduleStore>();
        services.AddSingleton<IPaymentGateway, LoggingPaymentGateway>();
        services.AddSingleton<LoanBalanceLedger>().AddScoped<AutopayService>().AddScoped<LoanOwnershipFilter>();
        return services;
    }
}
