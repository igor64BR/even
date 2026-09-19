using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class UsuarioEntityConfiguration : IEntityTypeConfiguration<UsuarioEntity>
{
    public void Configure(EntityTypeBuilder<UsuarioEntity> builder)
    {
        builder.ToTable("usuarios");

        builder.HasKey(usuario => usuario.Id);

        builder.Property(usuario => usuario.GoogleSubjectId)
            .HasMaxLength(255)
            .IsRequired();

        builder.HasIndex(usuario => usuario.GoogleSubjectId)
            .IsUnique();

        builder.Property(usuario => usuario.Nome)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(usuario => usuario.Email)
            .HasMaxLength(320)
            .IsRequired();

        builder.HasIndex(usuario => usuario.Email)
            .IsUnique();

        builder.Property(usuario => usuario.CriadoEm)
            .IsRequired();

        builder.HasMany(usuario => usuario.RefreshTokens)
            .WithOne(refreshToken => refreshToken.Usuario)
            .HasForeignKey(refreshToken => refreshToken.UsuarioId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
