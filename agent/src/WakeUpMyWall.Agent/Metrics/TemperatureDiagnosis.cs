namespace WakeUpMyWall.Agent.Metrics;

/**
 * 把"这块硬件上的温度传感器"翻译成一句人话结论，给 `--dump-sensors` 用。
 *
 * 为什么要单独一个纯函数：`—` 的原因有三种——名字没匹配上、有传感器但读数为空/0、
 * 这块硬件上根本没有温度传感器（后者才是权限/驱动问题）。这三种在真机上长得一样，
 * 但处理办法完全不同，所以把判定写成能单测的纯函数，而不是埋在只能上真机跑的 dump 循环里。
 */
public static class TemperatureDiagnosis
{
    public static string Describe(
        string label,
        IReadOnlyList<SensorReading> hardwareReadings,
        IReadOnlyList<string> candidateNames,
        Func<IReadOnlyList<SensorReading>, double?>? fallback = null)
    {
        var temperatures = hardwareReadings.Where(r => r.Sensor == SensorKind.Temperature).ToList();
        var matched = temperatures.LastOrDefault(r => candidateNames.Contains(r.Name) && r.Value > 0);

        if (matched is not null) return $"{label}：{matched.Value:0.#} ℃（传感器名 {matched.Name}）";

        if (fallback?.Invoke(hardwareReadings) is { } picked)
        {
            return $"{label}：{picked:0.#} ℃（候选名都没命中，兜底取了最高的温度传感器）";
        }

        if (temperatures.Count == 0) return $"{label}：这块硬件上一个温度传感器都没有";

        // 传感器在、值却是空/0：驱动能加载但这一项读不出来（例如没有 MSR 权限的时钟、没有 SMART 权限的盘温）。
        return $"{label}：有温度传感器但读数全空或为 0（{string.Join(", ", temperatures.Select(r => r.Name).Distinct())}）";
    }
}
