using System.Text;

namespace WakeUpMyWall.Agent.Logging;

/**
 * 极简文件日志。装成 Windows 服务后没有控制台，出问题时唯一能看的就是这个文件
 * （`%ProgramData%\WakeUpMyWall\agent.log`）——电源动作、指标读取失败、配对码都会写进去。
 *
 * 刻意做小：单文件追加 + 超过 2 MB 滚动一次（保留一份 .1）；任何 IO 失败都吞掉，
 * 日志写不进去绝不能把 Agent 弄挂。不改动 ASP.NET 自己的日志管线，只是额外挂一个 provider。
 */
public sealed class FileLoggerProvider(string path) : ILoggerProvider
{
    private const long MaxBytes = 2 * 1024 * 1024;

    private readonly object gate = new();

    public ILogger CreateLogger(string categoryName) => new FileLogger(this, categoryName);

    public void Dispose()
    {
        // 每次写入都是即开即关，没有需要释放的句柄。
    }

    private void Append(string line)
    {
        lock (gate)
        {
            try
            {
                var directory = Path.GetDirectoryName(path);
                if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);

                if (File.Exists(path) && new FileInfo(path).Length > MaxBytes)
                {
                    File.Move(path, path + ".1", overwrite: true);
                }

                File.AppendAllText(path, line + Environment.NewLine, Encoding.UTF8);
            }
            catch (Exception exception) when (exception is IOException or UnauthorizedAccessException or NotSupportedException)
            {
                // 日志不可写不是致命问题：Agent 照常干活。
            }
        }
    }

    private sealed class FileLogger(FileLoggerProvider provider, string category) : ILogger
    {
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => logLevel >= LogLevel.Information;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter)
        {
            if (!IsEnabled(logLevel)) return;

            var message = formatter(state, exception);
            var line = $"{DateTimeOffset.Now:yyyy-MM-dd HH:mm:ss.fff} [{logLevel}] {category}: {message}";
            if (exception is not null) line += $" | {exception.GetType().Name}: {exception.Message}";

            provider.Append(line);
        }
    }
}
