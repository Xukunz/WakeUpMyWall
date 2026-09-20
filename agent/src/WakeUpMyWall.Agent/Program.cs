using WakeUpMyWall.Agent.Api;
using WakeUpMyWall.Agent.Auth;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<ITokenStore>(new InMemoryTokenStore());

var app = builder.Build();

app.MapStatusEndpoints();

app.Run();

/** WebApplicationFactory<Program> 需要一个可引用的 Program 类型。 */
public partial class Program;
