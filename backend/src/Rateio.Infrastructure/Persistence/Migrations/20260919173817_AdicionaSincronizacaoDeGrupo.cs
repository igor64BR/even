using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Rateio.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class AdicionaSincronizacaoDeGrupo : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "EhConvidado",
                table: "participantes",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "Categoria",
                table: "grupos",
                type: "integer",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<Guid>(
                name: "DonoUsuarioId",
                table: "grupos",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<bool>(
                name: "Sincronizado",
                table: "grupos",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<DateOnly>(
                name: "Data",
                table: "despesas",
                type: "date",
                nullable: false,
                defaultValue: new DateOnly(1, 1, 1));

            migrationBuilder.CreateTable(
                name: "participacoes_despesa",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    DespesaId = table.Column<Guid>(type: "uuid", nullable: false),
                    ParticipanteId = table.Column<Guid>(type: "uuid", nullable: false),
                    Tipo = table.Column<int>(type: "integer", nullable: false),
                    Peso = table.Column<long>(type: "bigint", nullable: true),
                    ValorCentavos = table.Column<long>(type: "bigint", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_participacoes_despesa", x => x.Id);
                    table.ForeignKey(
                        name: "FK_participacoes_despesa_despesas_DespesaId",
                        column: x => x.DespesaId,
                        principalTable: "despesas",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_grupos_DonoUsuarioId",
                table: "grupos",
                column: "DonoUsuarioId");

            migrationBuilder.CreateIndex(
                name: "IX_participacoes_despesa_DespesaId",
                table: "participacoes_despesa",
                column: "DespesaId");

            migrationBuilder.AddForeignKey(
                name: "FK_grupos_usuarios_DonoUsuarioId",
                table: "grupos",
                column: "DonoUsuarioId",
                principalTable: "usuarios",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_grupos_usuarios_DonoUsuarioId",
                table: "grupos");

            migrationBuilder.DropTable(
                name: "participacoes_despesa");

            migrationBuilder.DropIndex(
                name: "IX_grupos_DonoUsuarioId",
                table: "grupos");

            migrationBuilder.DropColumn(
                name: "EhConvidado",
                table: "participantes");

            migrationBuilder.DropColumn(
                name: "Categoria",
                table: "grupos");

            migrationBuilder.DropColumn(
                name: "DonoUsuarioId",
                table: "grupos");

            migrationBuilder.DropColumn(
                name: "Sincronizado",
                table: "grupos");

            migrationBuilder.DropColumn(
                name: "Data",
                table: "despesas");
        }
    }
}
