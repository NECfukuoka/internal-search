# 地名検索機能

----
## 概要

地名検索機能は，日本国内の地名を検索するためのWeb ReST APIです．
本APIは，国土地理院より提供される地名データを格納したデータベースの検索結果を配信します．

Apache Tomcat (ver.8) 上で動作するアプリケーションとして実装しており，このアプリケーションをDockerで起動させます．
検索パラメータをURLに含めてGETメソッドでリクエストを送信すると，検索結果がGeoJSON形式で戻ります(検索時にエラー
が発生した場合もしくは検索結果が0件の場合は空配列が戻ります)．

Tomcatの起動時に，国土地理院のデータ(CSVファイル)をSQLiteのin-memory databaseとして読み込みます．
従って別途国土地理院のデータを入手しDocker Imageを生成する必要があります．


----
### 1. 地名検索用データの入手

地名検索用データファイルを入手します．

----
### 2. Dockerへの配備方法

1. GitHubよりZIPファイルとして本API一式をダウンロードし，Dockerの環境にコピーし，ZIPを展開します．

2. 先に取得した地名検索用データファイル(chimei_utf8.dat)を以下のディレクトリにコピーします．
```
address-search/src/main/webapp/WEB-INF/data/
```

3. 展開したフォルダ(address-search)直下に移動し，以下のdockerコマンドを実行
しDocker Imageを生成します．
```
$ docker build --no-cache -t address-search .
```

4. Docker Containerを配備(起動)します．
```
$ docker run -itd -p 8080:8080 --name address-search address-search
```
**ポート番号の部分は自身の環境にあわせて変更してください．**

5. 以下のURLをブラウザから送り動作確認します．
```
http://localhost:8080/address-search/AddressSearch?q=%E6%9D%B1%E4%BA%AC
```
(Dockerの実行されているOSからはlocalhostでアクセスできますが，他OSからの検証の場合はDockerの動作しているOSへのIP アドレス等を指定します)

 **なお上記URLのパラメータは「東京」をURLエンコードしたものです．**



----
## API仕様

APIへのリクエスト(URL)は以下となります：
```
http://[server]/address-search/AddressSearch?q=[検索文字列]
```
**文字列はUTF8文字列をエンコードします．**

レスポンスとしてGeoJSON( http://geojson.org/ )形式の値が戻ります(検索できない場合は空配列となります)．

例：リクエスト

```
http://localhost:8080/address-search/AddressSearch?q=%E6%9D%B1%E4%BA%AC
```

例：レスポンス(GeoJSON)

```
[{"geometry":{"coordinates":[139.343331472222,35.7550138055556],"type":"Point"},"type":"Feature","properties":{"addressCode":"13303","title":"東京水道"}}, ...
```
