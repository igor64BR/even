using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Tally.Application.Groups;

namespace Tally.Api.Hubs;

/// <summary>
/// T38.1: real-time notification hub (RF35/RF36; constitution.md principle 3 — our own system via
/// SignalR, no third-party push). The connection requires the same JWT Bearer already validated by
/// the rest of the API: <see cref="AuthorizeAttribute"/> reuses the default scheme configured in
/// <c>Tally.Infrastructure.DependencyInjection.AddJwtAuthentication</c> (T18) — the only difference
/// is that a Hub client doesn't send an <c>Authorization</c> header in the WebSocket handshake, so
/// the token arrives via the <c>access_token</c> query string (read by the <c>OnMessageReceived</c>
/// added in that same method, restricted to
/// <see cref="Infrastructure.Notifications.NotificationHubRoute.Path"/>).
///
/// One SignalR group per <c>groupId</c> (<see cref="Hub.Groups"/>) — no connection is subscribed
/// automatically; the client explicitly joins each synced group it's following via
/// <see cref="JoinGroupAsync"/>. Joining revalidates RNF07 (<see cref="GroupAccess.BelongsTo"/>, the
/// same rule <c>CreateExpenseUseCase</c>/<c>RegisterSettlementUseCase</c> already check before any
/// operation) — without this, any authenticated user could subscribe to another group's real-time
/// channel and see the description/amount of someone else's expenses and settlements just by
/// guessing the <c>groupId</c>.
/// </summary>
[Authorize]
public sealed class TallyHub(IGroupRepository groupRepository) : Hub
{
    public static string GroupName(Guid groupId) => $"group:{groupId}";

    public async Task JoinGroupAsync(Guid groupId)
    {
        var authenticatedUserId = GetAuthenticatedUserId();
        var access = await groupRepository.GetAccessAsync(groupId, Context.ConnectionAborted);

        if (access is null || !access.BelongsTo(authenticatedUserId))
        {
            // HubException is the only exception type whose Message reaches the SignalR client — we
            // don't leak the detail of "exists but isn't yours" vs. "doesn't exist" (the same care
            // use cases take via distinct exceptions doesn't apply here: in the Hub, distinguishing
            // the two cases would only help someone trying to enumerate other users' groupIds).
            throw new HubException("Group does not exist or access denied.");
        }

        await Groups.AddToGroupAsync(Context.ConnectionId, GroupName(groupId));
    }

    public Task LeaveGroupAsync(Guid groupId) =>
        Groups.RemoveFromGroupAsync(Context.ConnectionId, GroupName(groupId));

    private Guid GetAuthenticatedUserId()
    {
        // Same pattern as Tally.Api.Controllers.GroupsController.GetAuthenticatedUserId —
        // the "sub" claim is read verbatim because AddJwtAuthentication turns off MapInboundClaims.
        var subClaimValue = Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub)
            ?? throw new HubException("Authenticated JWT missing 'sub' claim.");

        return Guid.Parse(subClaimValue);
    }
}
