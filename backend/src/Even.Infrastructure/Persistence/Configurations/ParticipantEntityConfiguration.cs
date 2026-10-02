using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Persistence.Configurations;

public class ParticipantEntityConfiguration : IEntityTypeConfiguration<ParticipantEntity>
{
    public void Configure(EntityTypeBuilder<ParticipantEntity> builder)
    {
        builder.ToTable("participants");

        builder.HasKey(participant => participant.Id);

        builder.Property(participant => participant.Name)
            .HasMaxLength(120)
            .IsRequired();

        builder.Property(participant => participant.IsGuest)
            .IsRequired();
    }
}
