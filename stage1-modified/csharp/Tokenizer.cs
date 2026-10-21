using System.Diagnostics;
using System.Text.RegularExpressions;

namespace Stage1Modified;

public static class Tokenizer
{
    private static readonly Regex NonWord = new("[^a-z0-9\\s]+", RegexOptions.Compiled);

    public static HashSet<string> Tokenize(string text)
    {
        var normalized = NonWord.Replace(text.ToLowerInvariant(), " ");
        return normalized
            .Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries)
            .ToHashSet(StringComparer.Ordinal);
    }

    public static HashSet<string> TokenizeFile(string path) =>
        Tokenize(File.ReadAllText(path));

    public static StorageMetrics MeasureStorage(string path, Action operation)
    {
        var stopwatch = Stopwatch.StartNew();
        operation();
        stopwatch.Stop();
        long bytes = File.Exists(path)
            ? new FileInfo(path).Length
            : Directory.EnumerateFiles(path, "*", SearchOption.AllDirectories)
                .Sum(file => new FileInfo(file).Length);
        return new StorageMetrics(bytes, bytes / 1024d, stopwatch.Elapsed.TotalMilliseconds);
    }
}

public sealed record StorageMetrics(long SizeBytes, double SizeKb, double TimeMs);
