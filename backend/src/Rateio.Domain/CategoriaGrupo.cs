namespace Rateio.Domain;

/// <summary>
/// Categoria de um <see cref="Grupo"/> (ex.: viagem, casa compartilhada, casal). Não há um RF que
/// enumere valores fechados para isso ainda — modelado como enum, e não como
/// <see cref="string"/> wrapped, porque a T15 precisa de um valor pra popular <c>Grupo.Categoria</c>
/// e um conjunto fechado é mais rico que texto livre; ajustar/expandir os valores é decisão de UX
/// de outra task, não deste modelo de domínio.
/// </summary>
public enum CategoriaGrupo
{
    Viagem,
    Casa,
    Casal,
    Evento,
    Outro,
}
