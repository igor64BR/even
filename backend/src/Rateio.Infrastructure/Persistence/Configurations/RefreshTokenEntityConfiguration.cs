using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class RefreshTokenEntityConfiguration : IEntityTypeConfiguration<RefreshTokenEntity>
{
    public void Configure(EntityTypeBuilder<RefreshTokenEntity> builder)
    {
        builder.ToTable("refresh_tokens");

        builder.HasKey(refreshToken => refreshToken.Id);

        builder.Property(refreshToken => refreshToken.TokenHash)
            .HasMaxLength(128)
            .IsRequired();

        // Único: dois refresh tokens nunca colidem no mesmo hash (SHA-256 hex), e o índice também
        // acelera a busca por token no momento do refresh (T14).
        builder.HasIndex(refreshToken => refreshToken.TokenHash)
            .IsUnique();

        builder.Property(refreshToken => refreshToken.CriadoEm)
            .IsRequired();

        builder.Property(refreshToken => refreshToken.ExpiraEm)
            .IsRequired();
    }
}
