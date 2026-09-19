namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um código de convite (T21.1). Chave primária é o próprio
/// <see cref="Codigo"/> (não um <see cref="Guid"/> sintético): ele já precisa ser globalmente único
/// pra <c>POST /groups/join/{codigo}</c> resolver sem ambiguidade, então um id separado só
/// duplicaria essa garantia. Tabela própria (não coluna solta em <c>grupos</c>) porque o histórico
/// de "um código ativo por grupo, substituído a cada geração" (ver <c>Rateio.Application.Grupos.CodigoConvite</c>)
/// é mais natural como linhas de uma tabela do que como uma única coluna nullable sendo sobrescrita
/// — e deixa a porta aberta pra, no futuro, guardar códigos expirados/revogados sem migração de
/// schema.
/// </summary>
public class CodigoConviteEntity
{
    public string Codigo { get; set; } = string.Empty;

    public Guid GrupoId { get; set; }

    public DateTimeOffset CriadoEm { get; set; }

    public DateTimeOffset ExpiraEm { get; set; }

    public GrupoEntity? Grupo { get; set; }
}
