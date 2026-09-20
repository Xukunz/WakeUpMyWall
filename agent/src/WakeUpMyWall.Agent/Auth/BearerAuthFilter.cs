namespace WakeUpMyWall.Agent.Auth;

/**
 * 受保护端点的统一闸门：没有 Bearer Token 或 Token 不匹配一律 401，
 * 且**在调用端点处理器之前**就返回，所以不会产生任何副作用（spec §5 鉴权条款）。
 */
public sealed class BearerAuthFilter(ITokenStore tokens) : IEndpointFilter
{
    private const string Scheme = "Bearer ";

    public async ValueTask<object?> InvokeAsync(EndpointFilterInvocationContext context, EndpointFilterDelegate next)
    {
        var header = context.HttpContext.Request.Headers.Authorization.ToString();
        if (!header.StartsWith(Scheme, StringComparison.Ordinal) ||
            !tokens.Matches(header[Scheme.Length..].Trim()))
        {
            return Results.Json(
                new { error = "missing or invalid token" },
                statusCode: StatusCodes.Status401Unauthorized);
        }

        return await next(context);
    }
}
