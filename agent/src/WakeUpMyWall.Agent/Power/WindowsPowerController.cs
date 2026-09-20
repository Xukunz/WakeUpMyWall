using System.Diagnostics;

namespace WakeUpMyWall.Agent.Power;

/**
 * Windows 实现：只调用系统自带的关机/睡眠/锁屏命令。
 * 参数一律走 ArgumentList（参数数组），不拼字符串、不经过 cmd /c —— 这也是 spec §5 安全红线的一部分。
 */
public sealed class WindowsPowerController(ILogger<WindowsPowerController> logger) : IPowerController
{
    public Task<PowerResult> SleepAsync(CancellationToken ct) =>
        RunAsync("rundll32.exe", ["powrprof.dll,SetSuspendState", "0,1,0"], ct);

    public Task<PowerResult> ShutdownAsync(CancellationToken ct) =>
        RunAsync("shutdown.exe", ["/s", "/t", "0"], ct);

    public Task<PowerResult> RestartAsync(CancellationToken ct) =>
        RunAsync("shutdown.exe", ["/r", "/t", "0"], ct);

    public Task<PowerResult> LockAsync(CancellationToken ct) =>
        RunAsync("rundll32.exe", ["user32.dll,LockWorkStation"], ct);

    private async Task<PowerResult> RunAsync(string file, string[] arguments, CancellationToken ct)
    {
        var startInfo = new ProcessStartInfo(file)
        {
            UseShellExecute = false,
            CreateNoWindow = true,
        };
        foreach (var argument in arguments) startInfo.ArgumentList.Add(argument);

        using var process = Process.Start(startInfo);
        if (process is null) return new PowerResult(false, $"could not start {file}");

        await process.WaitForExitAsync(ct);
        logger.LogInformation(
            "{File} {Args} exited with {ExitCode}",
            file,
            string.Join(' ', arguments),
            process.ExitCode);

        return process.ExitCode == 0
            ? new PowerResult(true)
            : new PowerResult(false, $"{file} exited with {process.ExitCode}");
    }
}
