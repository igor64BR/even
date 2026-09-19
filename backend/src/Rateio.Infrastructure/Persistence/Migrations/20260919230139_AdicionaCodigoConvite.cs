using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rateio.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class AdicionaCodigoConvite : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "codigos_convite",
                columns: table => new
                {
                    Codigo = table.Column<string>(type: "character varying(12)", maxLength: 12, nullable: false),
                    GrupoId = table.Column<Guid>(type: "uuid", nullable: false),
                    CriadoEm = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    ExpiraEm = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_codigos_convite", x => x.Codigo);
                    table.ForeignKey(
                        name: "FK_codigos_convite_grupos_GrupoId",
                        column: x => x.GrupoId,
                        principalTable: "grupos",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_codigos_convite_GrupoId",
                table: "codigos_convite",
                column: "GrupoId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "codigos_convite");
        }
    }
}
