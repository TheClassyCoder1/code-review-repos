using Microsoft.Extensions.DependencyInjection;

namespace Lending.Servicing.Part3.Notifications;

public static class DunningRegistration
{
    /// <summary>The dispatcher only ever sees the channel sender through the dead-lettering wrapper.</summary>
    public static IServiceCollection AddDunning<TChannelSender, TDeadLetterStore>(this IServiceCollection services)
        where TChannelSender : class, IDunningSender
        where TDeadLetterStore : class, IDunningDeadLetterStore
    {
        services.AddSingleton<DunningQueue>();
        services.AddSingleton<TChannelSender>();
        services.AddSingleton<IDunningDeadLetterStore, TDeadLetterStore>();
        services.AddSingleton<IDunningSender>(sp => new DeadLetteringDunningSender(
            sp.GetRequiredService<TChannelSender>(), sp.GetRequiredService<IDunningDeadLetterStore>()));
        services.AddSingleton<DunningDispatcher>();
        return services;
    }
}
