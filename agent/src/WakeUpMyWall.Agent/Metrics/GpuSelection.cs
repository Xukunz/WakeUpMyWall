namespace WakeUpMyWall.Agent.Metrics;

/**
 * 多显卡机器上挑"主显卡"。之前的实现是"取枚举到的第一块"，于是带核显的电脑
 * （Intel UHD / AMD APU + 独显）经常显示成核显的数据 —— 用户实测踩到。
 *
 * 规则（从强到弱）：
 *  1. 厂商优先级：Nvidia / AMD 独显 > Intel 核显；
 *  2. 同厂商再看名字：带 `Radeon Graphics` / `Vega` / `UHD` / `Iris` 这类词的通常是核显，排后面；
 *  3. 仍并列时保持原顺序（稳定，不随枚举抖动）。
 *
 * 做成纯函数是为了能在 Linux/CI 上单测：Windows 适配层只负责把 LHM 的 (厂商, 名字) 喂进来。
 */
public static class GpuSelection
{
    private static readonly string[] IntegratedMarkers =
    [
        "Radeon Graphics", "Vega", "UHD", "Iris", "HD Graphics", "APU", "Integrated",
    ];

    public static int VendorPriority(string vendor) => vendor.ToLowerInvariant() switch
    {
        "nvidia" => 0,
        "amd" => 1,
        "intel" => 2,
        _ => 3,
    };

    public static bool LooksIntegrated(string name) =>
        IntegratedMarkers.Any(marker => name.Contains(marker, StringComparison.OrdinalIgnoreCase));

    /** 返回主显卡在 [candidates] 里的下标；空列表返回 -1。 */
    public static int PickPrimary(IReadOnlyList<(string Vendor, string Name)> candidates)
    {
        var best = -1;
        for (var index = 0; index < candidates.Count; index++)
        {
            if (best < 0)
            {
                best = index;
                continue;
            }

            // ValueTuple 之间没有 `<`：显式比较（先厂商优先级，再"是不是核显"）。
            var challenger = Rank(candidates[index]);
            var champion = Rank(candidates[best]);
            if (challenger.Vendor < champion.Vendor ||
                (challenger.Vendor == champion.Vendor && challenger.Integrated < champion.Integrated))
            {
                best = index;
            }
        }
        return best;
    }

    private static (int Vendor, int Integrated) Rank((string Vendor, string Name) candidate) =>
        (VendorPriority(candidate.Vendor), LooksIntegrated(candidate.Name) ? 1 : 0);
}
