Read Csv Xls Xlsx
==

### 逐行读，csv、xls和xlsx
```
注：csv-gbk乱码

ReadLine.read(File file, ReadLine.Listener listener);
ReadLine.read(文件, (行索引, <标题 => 值>) {});
```

### 分页读，csv、xls和xlsx
```
注：csv-gbk乱码

ReadPage.read(File file, ReadPage.Listener listener);
ReadPage.read(文件, (当前页的首行索引, [<标题 => 值>]) {});
```

### 同步读，csv、xls和xlsx
```
注：csv-gbk乱码

List<Map<String, String>> = ReadSync.read(File file);
[<标题 => 值>] = ReadSync.read(文件);
```

### 读csv
```
ReadCsv.readLine(File file, Charset charset, ReadLine.Listener listener);
ReadCsv.readLine(文件, 编码, (行索引, <标题 => 值>) {});

ReadCsv.readPage(File file, Charset charset, ReadPage.Listener listener)
ReadCsv.readPage(文件, 编码, (当前页的首行索引, [<标题 => 值>]) {});

List<Map<String, String>> = ReadCsv.readSync(File file, Charset charset);
[<标题 => 值>] = ReadCsv.readSync(文件, 编码);
```

### 检查文件前2048个字节编码
```
Utf8Utils checker = new Utf8Utils(2048);
boolean isUtf8 = checker.check(new File("a.csv"));
```
