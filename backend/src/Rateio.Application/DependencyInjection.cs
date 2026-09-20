using Microsoft.Extensions.DependencyInjection;
using Rateio.Application.Auth;
using Rateio.Application.Despesas;
using Rateio.Application.Grupos;
using Rateio.Application.Notificacoes;
using Rateio.Application.Quitacoes;
using Rateio.Application.Simplificacao;
using Rateio.Domain;

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
        services.AddScoped<RevogarSessaoUseCase>();
        services.AddScoped<SincronizarGrupoUseCase>();
        services.AddScoped<CriarDespesaUseCase>();
        services.AddScoped<GerarCodigoConviteUseCase>();
        services.AddScoped<EntrarNoGrupoViaConviteUseCase>();
        services.AddScoped<ObterSimplificacaoDeDividasUseCase>();
        services.AddScoped<RegistrarQuitacaoUseCase>();
        services.AddScoped<ObterEventosDeGrupoUseCase>();

        // T32: o motor de simplificação (Rateio.Domain, T31) não tem estado — Singleton evita uma
        // instância nova por requisição sem ganhar nada em troca. Registrado aqui (Application,
        // não Infrastructure) porque é o motor do domínio, não um detalhe de infraestrutura; e não
        // em Rateio.Domain porque esse projeto não depende de Microsoft.Extensions.DependencyInjection
        // (Domain sem dependências externas é decisão estrutural do projeto).
        services.AddSingleton<IMotorDeSimplificacaoDeDividas, MotorDeSimplificacaoDeDividas>();

        return services;
    }
}
