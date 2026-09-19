using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class CodigoConviteEntityConfiguration : IEntityTypeConfiguration<CodigoConviteEntity>
{
    public void Configure(EntityTypeBuilder<CodigoConviteEntity> builder)
    {
        builder.ToTable("codigos_convite");

        builder.HasKey(codigo => codigo.Codigo);

        builder.Property(codigo => codigo.Codigo)
            .HasMaxLength(12)
            .IsRequired();

        builder.Property(codigo => codigo.GrupoId)
            .IsRequired();

        builder.Property(codigo => codigo.CriadoEm)
            .IsRequired();

        builder.Property(codigo => codigo.ExpiraEm)
            .IsRequired();

        builder.HasIndex(codigo => codigo.GrupoId);

        // Cascade: se o grupo é excluído (RF15), o código de convite dele não tem mais sentido —
        // diferente do dono de um grupo (Restrict em GrupoEntityConfiguration), que é um vínculo
        // que não deve sumir só porque uma operação secundária apagou algo.
        builder.HasOne(codigo => codigo.Grupo)
            .WithMany()
            .HasForeignKey(codigo => codigo.GrupoId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
