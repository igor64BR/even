namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de uma quitação (RF31/RF33). T32 cria esta tabela só para o caminho
/// de leitura — <c>GET /groups/{id}/settlement</c> soma quitações vigentes ao saldo do grupo, a
/// mesma regra que o motor de simplificação (T31) já aplica em <c>ComputeBalances</c>. Registrar
/// uma quitação nova (o caminho de escrita) é escopo de T35; esta entidade e sua configuration já
/// ficam prontas para isso, sem precisar de uma segunda migração para os mesmos campos.
///
/// <see cref="ValorCentavos"/> guarda o mesmo valor bruto que <see cref="Rateio.Domain.Dinheiro.Centavos"/>
/// expõe, mesma decisão de <see cref="DespesaEntity.ValorTotalCentavos"/> — nunca vira
/// <c>decimal</c> cru na coluna (algorithm-spec.md, seção "Dinheiro").
/// </summary>
public class QuitacaoEntity
{
    public Guid Id { get; set; }

    public Guid GrupoId { get; set; }

    public Guid PagadorId { get; set; }

    public Guid RecebedorId { get; set; }

    public long ValorCentavos { get; set; }

    public DateTimeOffset CriadoEm { get; set; }

    public GrupoEntity? Grupo { get; set; }
}
