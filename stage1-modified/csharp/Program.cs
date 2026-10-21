using System.Diagnostics;
using System.Globalization;
using System.Text.Json;
using Stage1Modified;

CultureInfo.CurrentCulture = CultureInfo.InvariantCulture;
var corpus = args.Length > 0 ? args[0] : Path.Combine("..", "benchmarks", "corpus");
var output = args.Length > 1 ? args[1] : Path.Combine("..", "benchmarks", "results-csharp.csv");
Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(output))!);
var books = File.ReadLines(Path.Combine(corpus, "manifest.csv")).Skip(1)
    .Select(line => line.Split(','))
    .Select(fields => new Book(int.Parse(fields[0]), fields[1], fields[2]))
    .ToList();
var rows = new List<string> { "language,area,structure,operation,books,bytes,files,directories,time_ms,terms,postings,query,answer_count,status" };

foreach (var layout in new[] { "by_book", "by_time", "by_batch" })
{
    var root = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(output))!, $"csharp-run-{layout}");
    if (Directory.Exists(root)) Directory.Delete(root, true);
    var watch = Stopwatch.StartNew();
    foreach (var book in books)
    {
        var folder = layout switch
        {
            "by_book" => Path.Combine(root, book.Id.ToString()),
            "by_time" => Path.Combine(root, "1970-01-01"),
            _ => Path.Combine(root, "batch-1")
        };
        Directory.CreateDirectory(folder);
        File.Copy(Path.Combine(corpus, book.Header), Path.Combine(folder, $"{book.Id}.header.txt"));
        File.Copy(Path.Combine(corpus, book.Body), Path.Combine(folder, $"{book.Id}.body.txt"));
    }
    watch.Stop();
    var stat = Stat(root);
    rows.Add($"csharp,datalake,{layout},write,{books.Count},{stat.bytes},{stat.files},{stat.directories},{watch.Elapsed.TotalMilliseconds:F4},,,,,PASS");
}

var index = new Dictionary<string, HashSet<int>>(StringComparer.Ordinal);
var buildWatch = Stopwatch.StartNew();
foreach (var book in books)
{
    foreach (var token in Tokenizer.TokenizeFile(Path.Combine(corpus, book.Body)))
    {
        if (!index.TryGetValue(token, out var ids)) index[token] = ids = new HashSet<int>();
        ids.Add(book.Id);
    }
}
buildWatch.Stop();
var postings = index.Values.Sum(ids => ids.Count);
foreach (var structure in new[] { "monolithic-json", "hierarchy-json" })
{
    var root = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(output))!, $"csharp-index-{structure}");
    if (Directory.Exists(root)) Directory.Delete(root, true);
    Directory.CreateDirectory(root);
    var persistWatch = Stopwatch.StartNew();
    if (structure == "monolithic-json")
        File.WriteAllText(Path.Combine(root, "index.json"), JsonSerializer.Serialize(index.ToDictionary(x => x.Key, x => x.Value.Order().ToArray())));
    else
        foreach (var bucket in index.GroupBy(pair => char.IsLetterOrDigit(pair.Key[0]) ? pair.Key[0].ToString() : "misc"))
        {
            var folder = Path.Combine(root, bucket.Key);
            Directory.CreateDirectory(folder);
            var values = bucket.ToDictionary(pair => pair.Key, pair => pair.Value.Order().ToArray());
            File.WriteAllText(Path.Combine(folder, "index.json"), JsonSerializer.Serialize(values));
        }
    persistWatch.Stop();
    var stat = Stat(root);
    var answerCount = index.TryGetValue("adventure", out var answer) ? answer.Count : 0;
    rows.Add($"csharp,index,{structure},build,{books.Count},{stat.bytes},{stat.files},{stat.directories},{buildWatch.Elapsed.TotalMilliseconds:F4},{index.Count},{postings},adventure,{answerCount},PASS");
    rows.Add($"csharp,index,{structure},persist,{books.Count},{stat.bytes},{stat.files},{stat.directories},{persistWatch.Elapsed.TotalMilliseconds:F4},{index.Count},{postings},,,PASS");
    var queryWatch = Stopwatch.StartNew();
    var queryCount = structure == "monolithic-json"
        ? JsonSerializer.Deserialize<Dictionary<string, int[]>>(File.ReadAllText(Path.Combine(root, "index.json")))!["adventure"].Length
        : Directory.EnumerateFiles(root, "index.json", SearchOption.AllDirectories)
            .Select(file => JsonSerializer.Deserialize<Dictionary<string, int[]>>(File.ReadAllText(file))!)
            .Where(value => value.ContainsKey("adventure")).SelectMany(value => value["adventure"]).Count();
    queryWatch.Stop();
    rows.Add($"csharp,index,{structure},query,{books.Count},{stat.bytes},{stat.files},{stat.directories},{queryWatch.Elapsed.TotalMilliseconds:F4},{index.Count},{postings},adventure,{queryCount},PASS");
}

var updateWatch = Stopwatch.StartNew();
foreach (var token in Tokenizer.TokenizeFile(Path.Combine(corpus, books[0].Body)))
{
    if (!index.TryGetValue(token, out var ids)) index[token] = ids = new HashSet<int>();
    ids.Add(99);
}
updateWatch.Stop();
rows.Add($"csharp,index,in-memory,incremental_update,1,0,0,0,{updateWatch.Elapsed.TotalMilliseconds:F4},{index.Count},{index.Values.Sum(x => x.Count)},,,PASS");
File.WriteAllLines(output, rows);

static (long bytes, int files, int directories) Stat(string root)
{
    if (!Directory.Exists(root)) return (0, 0, 0);
    var files = Directory.EnumerateFiles(root, "*", SearchOption.AllDirectories).ToArray();
    var directories = Directory.EnumerateDirectories(root, "*", SearchOption.AllDirectories).Count();
    return (files.Sum(path => new FileInfo(path).Length), files.Length, directories);
}

record Book(int Id, string Header, string Body);
