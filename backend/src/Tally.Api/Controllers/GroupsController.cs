using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Tally.Api.Contracts;
using Tally.Application.Expenses;
using Tally.Application.Groups;
using Tally.Application.Notifications;
using Tally.Application.Settlements;
using Tally.Application.Simplification;

namespace Tally.Api.Controllers;

/// <summary>
/// Sync of a local group (Room, app) to the backend, plus day-to-day group operations:
/// <c>POST /groups/{id}/expenses</c> (adding a single expense to a group that's already synced,
/// different from the sync bulk load); the group invite/link,
/// <c>POST /groups/{id}/invite-code</c> (owner only) and <c>POST /groups/join/{code}</c> (any
/// authenticated user); <c>GET /groups/{id}/settlement</c>, the group's debt
/// simplification, recomputed on demand (never stored); and
/// <c>POST /groups/{id}/settlements</c>, which records that one of the suggested transactions was
/// paid. The controller only translates HTTP&lt;-&gt;use case; all orchestration lives in the use
/// cases (<see cref="SyncGroupUseCase"/>, <see cref="CreateExpenseUseCase"/>,
/// <see cref="GenerateInviteCodeUseCase"/>, <see cref="JoinGroupViaInviteUseCase"/>,
/// <see cref="GetDebtSimplificationUseCase"/>, <see cref="RegisterSettlementUseCase"/>). Also
/// exposes <c>GET /groups/{id}/events?since={timestampIso8601}</c>: pull fallback for
/// expense/settlement events missed while the app was disconnected from the SignalR Hub — see
/// <see cref="GetGroupEventsUseCase"/>. And
/// <c>PUT</c>/<c>DELETE /groups/{id}/expenses/{expenseId}</c> (edit/delete an expense — see
/// <see cref="EditExpenseUseCase"/>/<see cref="DeleteExpenseUseCase"/>) and
/// <c>GET /groups/{id}</c> (full group — see <see cref="GetGroupUseCase"/>).
/// </summary>
[ApiController]
[Authorize]
[Route("groups")]
public class GroupsController(
    SyncGroupUseCase syncGroup,
    CreateExpenseUseCase createExpense,
    EditExpenseUseCase editExpense,
    DeleteExpenseUseCase deleteExpense,
    GenerateInviteCodeUseCase generateInviteCode,
    JoinGroupViaInviteUseCase joinGroupViaInvite,
    GetGroupUseCase getGroup,
    GetDebtSimplificationUseCase getDebtSimplification,
    RegisterSettlementUseCase registerSettlement,
    GetGroupEventsUseCase getGroupEvents) : ControllerBase
{
    /// <summary>
    /// The synced group's owner is always the user from the validated JWT
    /// (<see cref="GetAuthenticatedUserId"/>), never a field from the request body — there's no way
    /// for an authenticated user to sync a group on behalf of another.
    /// </summary>
    [HttpPost("sync")]
    public async Task<ActionResult<SyncGroupResponse>> Sync(
        [FromBody] SyncGroupRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var groupId = await syncGroup.ExecuteAsync(authenticatedUserId, request, cancellationToken);

            return Ok(new SyncGroupResponse(groupId));
        }
        catch (Exception error) when (error is ArgumentException or InvalidOperationException)
        {
            // Malformed payload or one that violates a domain invariant (e.g. empty name, group
            // with no participant, weighted split with no weight) — never a server error.
            return Problem(
                title: "Invalid sync payload.",
                detail: error.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    /// <summary>
    /// Adds an expense to an already-synced group (<paramref name="id"/> is the server-side
    /// <c>GroupId</c> returned by <c>POST /groups/sync</c>). Access is checked in the use case
    /// (<see cref="CreateExpenseUseCase"/>) before any persistence — 404 if the group doesn't
    /// exist, 403 if the authenticated user doesn't have access to it.
    /// </summary>
    [HttpPost("{id:guid}/expenses")]
    public async Task<ActionResult<CreateExpenseResponse>> CreateExpense(
        Guid id,
        [FromBody] SyncedExpenseRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var expenseId = await createExpense.ExecuteAsync(authenticatedUserId, id, request, cancellationToken);

            return Created($"/groups/{id}/expenses/{expenseId}", new CreateExpenseResponse(expenseId));
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
        catch (Exception error) when (error is ArgumentException or InvalidOperationException)
        {
            // Same pattern as Sync: malformed payload or one that violates a domain invariant is
            // never a server error.
            return Problem(
                title: "Invalid expense payload.",
                detail: error.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    /// <summary>
    /// Edits an already-persisted expense. Same access/error pattern as
    /// <see cref="CreateExpense"/>: 404 if the group doesn't exist, 403 without access;
    /// adds 404 when the group exists but <paramref name="expenseId"/> doesn't match one of its
    /// expenses (<see cref="ExpenseNotFoundException"/>). Rebuilds the whole expense from the
    /// payload (<see cref="EditExpenseUseCase"/>) — never a partial loose-field update.
    /// </summary>
    [HttpPut("{id:guid}/expenses/{expenseId:guid}")]
    public async Task<IActionResult> EditExpense(
        Guid id,
        Guid expenseId,
        [FromBody] SyncedExpenseRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            await editExpense.ExecuteAsync(authenticatedUserId, id, expenseId, request, cancellationToken);

            return NoContent();
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (ExpenseNotFoundException error)
        {
            return Problem(
                title: "Expense not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
        catch (Exception error) when (error is ArgumentException or InvalidOperationException)
        {
            // Same pattern as CreateExpense: malformed payload or one that violates a domain
            // invariant is never a server error.
            return Problem(
                title: "Invalid expense payload.",
                detail: error.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    /// <summary>
    /// Deletes an already-persisted expense. Same access/error pattern as
    /// <see cref="EditExpense"/>: 404 if the group doesn't exist, 404 if <paramref name="expenseId"/>
    /// doesn't match an expense of that group, 403 without access.
    /// </summary>
    [HttpDelete("{id:guid}/expenses/{expenseId:guid}")]
    public async Task<IActionResult> DeleteExpense(Guid id, Guid expenseId, CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            await deleteExpense.ExecuteAsync(authenticatedUserId, id, expenseId, cancellationToken);

            return NoContent();
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (ExpenseNotFoundException error)
        {
            return Problem(
                title: "Expense not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// Full group — name, category, participants, and expenses (with description/amount/
    /// payer/date/split): <c>POST /groups/join/{code}</c> only returns <c>{groupId}</c>, so the app
    /// needs this endpoint to fetch the rest afterward. Same
    /// access/error pattern as <see cref="CreateExpense"/>: 404 if the group doesn't exist, 403
    /// without access.
    /// </summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<GetGroupResponse>> GetGroup(Guid id, CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var fullGroup = await getGroup.ExecuteAsync(authenticatedUserId, id, cancellationToken);

            return Ok(GroupResponseMapper.Build(fullGroup));
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// Generates (or replaces — see <c>InviteCode</c>, "one active code per group") the
    /// invite code for group <paramref name="id"/>. Only the owner generates a code —
    /// <see cref="GenerateInviteCodeUseCase"/> throws <see cref="AccessDeniedException"/> (403) for
    /// any other authenticated user, even one who's already a participant.
    /// </summary>
    [HttpPost("{id:guid}/invite-code")]
    public async Task<ActionResult<GenerateInviteCodeResponse>> GenerateInviteCode(
        Guid id,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var code = await generateInviteCode.ExecuteAsync(authenticatedUserId, id, cancellationToken);

            return Ok(new GenerateInviteCodeResponse(code.Value, code.ExpiresAt));
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "Only the group owner can generate an invite code.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// The group's debt simplification, always recomputed from the current history of
    /// expenses and settlements (this endpoint stores no balance at all). Same
    /// access/error pattern as <see cref="CreateExpense"/>: 404 if the group doesn't exist, 403 if
    /// the authenticated user isn't its owner.
    /// </summary>
    [HttpGet("{id:guid}/settlement")]
    public async Task<ActionResult<IReadOnlyList<TransactionResponse>>> GetSettlement(
        Guid id,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var transactions = await getDebtSimplification.ExecuteAsync(authenticatedUserId, id, cancellationToken);

            var response = transactions
                .Select(transaction => new TransactionResponse(transaction.From.Value, transaction.To.Value, transaction.Amount.Cents))
                .ToList();

            return Ok(response);
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// Records that one of the transactions suggested by <see cref="GetSettlement"/> was paid —
    /// <paramref name="request"/> is
    /// <c>{ fromParticipantId, toParticipantId, amountCents }</c>. Same access/error pattern as
    /// <see cref="CreateExpense"/>: 404 if the group doesn't exist, 403 if the authenticated user
    /// doesn't have access to it, 400 if the payload violates a domain invariant
    /// (non-positive amount, payer equal to payee). Doesn't return a recomputed balance — the next
    /// call to <c>GET /groups/{id}/settlement</c> already reflects the settlement.
    /// </summary>
    [HttpPost("{id:guid}/settlements")]
    public async Task<ActionResult<RegisterSettlementResponse>> RegisterSettlement(
        Guid id,
        [FromBody] RegisterSettlementRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var settlementId = await registerSettlement.ExecuteAsync(authenticatedUserId, id, request, cancellationToken);

            return Created($"/groups/{id}/settlements/{settlementId}", new RegisterSettlementResponse(settlementId));
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
        catch (ArgumentException error)
        {
            // Same pattern as CreateExpense: a payload that violates a domain invariant is never a
            // server error.
            return Problem(
                title: "Invalid settlement payload.",
                detail: error.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    /// <summary>
    /// <c>GET /groups/{id}/events?since={timestampIso8601}</c> — pull fallback: the group's
    /// expense/settlement events persisted on the server after <paramref name="since"/>, ordered by
    /// ascending date, in the same format already sent in real time via SignalR
    /// (<see cref="ExpenseCreatedEvent"/>/<see cref="DebtSettledEvent"/>) — the app uses this to
    /// recover what it missed while disconnected from the Hub. Same
    /// access/error pattern as <see cref="GetSettlement"/>: 404 if the group doesn't exist, 403 if
    /// the authenticated user doesn't have access to it. A group with no new events returns
    /// an empty list (200), never 404.
    ///
    /// The response is typed as <c>IReadOnlyList&lt;object&gt;</c> (not
    /// <c>IReadOnlyList&lt;IGroupEvent&gt;</c>) because <c>System.Text.Json</c> serializes each
    /// element of a collection by its declared type, not by its runtime concrete type — returning
    /// the interface would make the serializer include only <c>Type</c>/<c>GroupId</c> (the
    /// interface's members) and drop <c>Description</c>/<c>TotalAmountCents</c>/etc. Declaring each
    /// item as <c>object</c> forces the serializer to use each event's concrete type — the same care
    /// <c>SignalRGroupEventNotifier</c> already takes, just there via a <c>switch</c>
    /// dispatching to the concrete type on every call to <c>SendAsync</c>.
    /// </summary>
    [HttpGet("{id:guid}/events")]
    public async Task<ActionResult<IReadOnlyList<object>>> GetEvents(
        Guid id,
        [FromQuery] DateTimeOffset since,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserId = GetAuthenticatedUserId();

            var events = await getGroupEvents.ExecuteAsync(authenticatedUserId, id, since, cancellationToken);

            return Ok(events.Cast<object>().ToList());
        }
        catch (GroupNotFoundException error)
        {
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (AccessDeniedException error)
        {
            return Problem(
                title: "User does not have access to the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status403Forbidden);
        }
    }

    /// <summary>
    /// Resolves <paramref name="code"/> and adds the authenticated user as a new participant
    /// of the corresponding group. Open to any authenticated user — there's no owner/access
    /// check here, the valid code IS the authorization: joining via code is open to any
    /// authenticated user, that's the point of the feature. Response in the same format as
    /// <c>POST /groups/sync</c>, so the app can already show the group.
    /// </summary>
    [HttpPost("join/{code}")]
    public async Task<ActionResult<SyncGroupResponse>> JoinWithCode(
        string code,
        CancellationToken cancellationToken)
    {
        try
        {
            var authenticatedUserName = GetAuthenticatedUserName();

            var groupId = await joinGroupViaInvite.ExecuteAsync(code, authenticatedUserName, cancellationToken);

            return Ok(new SyncGroupResponse(groupId));
        }
        catch (InvalidInviteCodeException error)
        {
            return Problem(
                title: "Invalid invite code.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (GroupNotFoundException error)
        {
            // Defensive: this would only happen if the invite's group had been deleted between
            // resolving the code and reading the group — the schema already protects against this
            // via cascade (InviteCodeEntityConfiguration), but the use case doesn't silently assume
            // that.
            return Problem(
                title: "Group not found.",
                detail: error.Message,
                statusCode: StatusCodes.Status404NotFound);
        }
        catch (Exception error) when (error is ArgumentException or InvalidOperationException)
        {
            return Problem(
                title: "Could not join the group.",
                detail: error.Message,
                statusCode: StatusCodes.Status400BadRequest);
        }
    }

    private Guid GetAuthenticatedUserId()
    {
        var subClaimValue = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
            ?? throw new InvalidOperationException("Authenticated JWT missing 'sub' claim.");

        return Guid.Parse(subClaimValue);
    }

    /// <summary>
    /// The new <c>Participant.Authenticated</c>'s name comes from the JWT's <c>name</c>
    /// claim — the same value <c>JwtIssuer</c> writes from <c>User.Name</c> when issuing the
    /// token, without needing a new query to <c>IUserRepository</c> just to get the name back.
    /// </summary>
    private string GetAuthenticatedUserName() =>
        User.FindFirstValue(JwtRegisteredClaimNames.Name)
            ?? throw new InvalidOperationException("Authenticated JWT missing 'name' claim.");
}
