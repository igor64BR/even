using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class ParticipacaoDespesaEntityConfiguration : IEntityTypeConfiguration<ParticipacaoDespesaEntity>
{
    public void Configure(EntityTypeBuilder<ParticipacaoDespesaEntity> builder)
    {
        builder.ToTable("participacoes_despesa");

        builder.HasKey(participacao => participacao.Id);

        builder.Property(participacao => participacao.ParticipanteId)
            .IsRequired();

        builder.Property(participacao => participacao.Tipo)
            .IsRequired();
    }
}
