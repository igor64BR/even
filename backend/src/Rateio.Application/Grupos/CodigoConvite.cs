namespace Rateio.Application.Grupos;

/// <summary>
/// Código de convite de um grupo sincronizado (T21.1/RF07). Decisões de produto documentadas aqui
/// porque não há outro lugar óbvio pra elas viverem:
///
/// <list type="bullet">
/// <item><b>Reutilizável, não de uso único.</b> É o mecanismo de link/convite do grupo (RF07) —
/// várias pessoas devem poder entrar com o mesmo código, do contrário o dono precisaria gerar (e
/// redistribuir) um código novo pra cada convidado, o que não é o modelo mental de "link do
/// grupo".</item>
/// <item><b>Um código ativo por grupo.</b> Gerar um novo código (chamar o endpoint de novo)
/// substitui o anterior (<see cref="ICodigoConviteRepository.SalvarAsync"/> faz upsert por
/// <see cref="GrupoId"/>) — evita códigos antigos esquecidos e ainda válidos circulando, e dá ao
/// dono uma forma simples de "revogar" um link comprometido: gerar outro.</item>
/// <item><b>Expira em 7 dias.</b> Não precisa ser de uso único pra ser seguro o bastante pra um
/// MVP (T21 explicitamente não exige força de token de sessão), mas um código sem validade
/// nenhuma ficaria value pra sempre se vazasse. 7 dias é tempo de sobra pra um convite chegar ao
/// convidado e ele entrar, sem o código virar um segredo de longuíssimo prazo.</item>
/// </list>
/// </summary>
public sealed record CodigoConvite(string Valor, Guid GrupoId, DateTimeOffset CriadoEm, DateTimeOffset ExpiraEm)
{
    public bool EstaValido(DateTimeOffset agora) => agora < ExpiraEm;
}
