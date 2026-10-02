using Microsoft.Extensions.DependencyInjection;
using Even.Application.Auth;
using Even.Application.Expenses;
using Even.Application.Groups;
using Even.Application.Notifications;
using Even.Application.Settlements;
using Even.Application.Simplification;
using Even.Domain;

namespace Even.Application;

/// <summary>
/// DI registration point for the Application layer. Registers use cases — never concrete
/// Infrastructure implementations, which are registered in
/// <c>Even.Infrastructure.DependencyInjection</c>.
/// </summary>
public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<AuthenticateWithGoogleUseCase>();
        services.AddScoped<RevokeSessionUseCase>();
        services.AddScoped<SyncGroupUseCase>();
        services.AddScoped<CreateExpenseUseCase>();
        services.AddScoped<EditExpenseUseCase>();
        services.AddScoped<DeleteExpenseUseCase>();
        services.AddScoped<GenerateInviteCodeUseCase>();
        services.AddScoped<JoinGroupViaInviteUseCase>();
        services.AddScoped<GetGroupUseCase>();
        services.AddScoped<GetDebtSimplificationUseCase>();
        services.AddScoped<RegisterSettlementUseCase>();
        services.AddScoped<GetGroupEventsUseCase>();

        // The simplification engine (Even.Domain) has no state — Singleton avoids a
        // new instance per request for no benefit in return. Registered here (Application, not
        // Infrastructure) because it's the domain's engine, not an infrastructure detail; and not
        // in Even.Domain because that project doesn't depend on
        // Microsoft.Extensions.DependencyInjection (Domain having no external dependencies is a
        // structural decision of the project).
        services.AddSingleton<IDebtSimplificationEngine, DebtSimplificationEngine>();

        return services;
    }
}
