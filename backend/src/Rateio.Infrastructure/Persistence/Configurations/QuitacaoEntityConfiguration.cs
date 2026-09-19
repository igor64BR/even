using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class QuitacaoEntityConfiguration : IEntityTypeConfiguration<QuitacaoEntity>
{
    public void Configure(EntityTypeBuilder<QuitacaoEntity> builder)
    {
        builder.ToTable("quitacoes");

        builder.HasKey(quitacao => quitacao.Id);

        builder.Property(quitacao => quitacao.PagadorId)
            .IsRequired();

        builder.Property(quitacao => quitacao.RecebedorId)
            .IsRequired();

        builder.Property(quitacao => quitacao.ValorCentavos)
            .IsRequired();

        builder.Property(quitacao => quitacao.CriadoEm)
            .IsRequired();

        // Sem coleção de navegação em GrupoEntity (mesmo padrão de GrupoEntity.Dono): nada hoje
        // precisa carregar "o grupo com suas quitações" de uma vez, só filtrar quitações por
        // GrupoId — QuitacaoRepository (T32) consulta QuitacaoEntity diretamente.
        builder.HasOne(quitacao => quitacao.Grupo)
            .WithMany()
            .HasForeignKey(quitacao => quitacao.GrupoId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
