using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rateio.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class AdicionaQuitacao : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "quitacoes",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    GrupoId = table.Column<Guid>(type: "uuid", nullable: false),
                    PagadorId = table.Column<Guid>(type: "uuid", nullable: false),
                    RecebedorId = table.Column<Guid>(type: "uuid", nullable: false),
                    ValorCentavos = table.Column<long>(type: "bigint", nullable: false),
                    CriadoEm = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_quitacoes", x => x.Id);
                    table.ForeignKey(
                        name: "FK_quitacoes_grupos_GrupoId",
                        column: x => x.GrupoId,
                        principalTable: "grupos",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_quitacoes_GrupoId",
                table: "quitacoes",
                column: "GrupoId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "quitacoes");
        }
    }
}
