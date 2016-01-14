# 地名検索機能

----
## 概要

地名検索機能は，地名を検索するためのWeb ReST APIです．
本APIは，東京大学CSIS シンプルジオコーディング実験サービス及び地名データを格納したデータベースで検索した結果を配信します．

Apache Tomcat (ver.8) 上で動作するアプリケーションとして実装しており，このアプリケーションをDockerで起動させます．
検索パラメータをURLに含めてGETメソッドでリクエストを送信すると，検索結果がGeoJSON形式で戻ります(検索時にエラーが発生した場合もしくは検索結果が0件の場合は空配列が戻ります)．

Tomcatの起動時に，地名データ(CSVファイル)をSQLiteのin-memory databaseとして読み込みます．
地名データを別途用意してDocker Imageを生成する必要があります．


----
### 1. 地名データの用意

地名データを用意します．
地名データはCSVファイルで以下の情報を上から下の順にカンマ(,)区切りで記載します．

カラム番号  |  内容 | 例
---- | ---- | ----
1  |※空きカラム(未使用)※|
2  |「地名 or 公共施設名」のフラグ(「1」…地名、「2」…公共施設名)|1
3  |タイトル|下谷
4  |※空きカラム(未使用)※|
5  |市区町村コード(最初の桁が「0」の場合は除かれている)|18205
6  |経度(10進法)|136.74747
7  |緯度(10進法)|35.951299
8  |※空きカラム(未使用)※|
9  |※空きカラム(未使用)※|
10  |※空きカラム(未使用)※|

ファイル名「chimei_utf8.dat」、文字コード「UTF-8」、改行コード「LF」で作成します．

----
### 2. Dockerへの配備方法

1. GitHubよりZIPファイルとして本API一式をダウンロードし，Dockerの環境にコピーし，ZIPを展開します．

2. 用意した地名データ(chimei_utf8.dat)を以下のディレクトリにコピーします．
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


----
## gsimaps(地理院地図)での動作確認用サンプル
sampleフォルダ配下に、地名検索機能をgsimaps(地理院地図)で動作させるサンプルを格納しています．

本サンプルコードの動作には、以下で公開している3つの機能を必要とします．
 リバースジオコーダ機能：https://github.com/gsi-cyberjapan/internal-reversegeocoder
 標高API：https://github.com/gsi-cyberjapan/internal-elevation
 カウンタ機能：https://github.com/gsi-cyberjapan/internal-counter
地名検索機能、及び上記3つの機能の呼び出し先は、sample/gsimaps-gh-pages/js/gsimaps.js のファイルで定義する必要があります．
```
CONFIG.SERVERAPI.ACCESSCOUNTER = 'http://localhost:8083/CounterJson.php';
CONFIG.SERVERAPI.GETADDR = "http://localhost:8081/reverse-geocoder/LonLatToAddress";
CONFIG.SERVERAPI.GETELEVATION = "http://localhost:8082/getelevation.php";
CONFIG.SERVERAPI.CHIMEI_SEARCH="http://localhost:8080/address-search/AddressSearch";
```
