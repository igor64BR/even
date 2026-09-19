using Microsoft.Extensions.DependencyInjection;
using Rateio.Application.Auth;
using Rateio.Application.Grupos;

namespace Rateio.Application;

/// <summary>
/// Ponto de registro de DI da camada de Application. Registra casos de uso — nunca implementações
/// concretas de Infrastructure, que se registram em <c>Rateio.Infrastructure.DependencyInjection</c>.
/// </summary>
public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<AutenticarComGoogleUseCase>();
        services.AddScoped<SincronizarGrupoUseCase>();

        return services;
    }
}
