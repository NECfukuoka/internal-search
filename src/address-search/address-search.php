<?php
/**
 * 東京大学CSIS シンプルジオコーディング実験サービス　地理院地図連携　サンプルコード
 */
ini_set('default_charset', 'UTF-8');
if (preg_match('/\b(gzip|deflate)\b/i', $_SERVER['HTTP_ACCEPT_ENCODING'], $matches)) {
	// 圧縮可能であれば圧縮する
	ini_set("zlib.output_compression","On");
}
$outputData = array();

$searchWord = $_GET["q"];
if (strlen(trim($searchWord)) == 0) {
	return sendResult($outputData);
}
$searchWord = htmlspecialchars(trim($searchWord), ENT_QUOTES, 'UTF-8');

require "csis.php";

$dataSourceList = array(
	//東京大学CSIS シンプルジオコーディング実験サービス 住所
	"csisAddress" => new CSISAddressDataSource(),
);

foreach ($dataSourceList as $key => $dataSource) {
	$result = $dataSource->search($searchWord);
	if ($result !== false) {
		$outputData = array_merge($outputData,$result);
	}
}

sendResult($outputData);

/**
 * JSONデータを送信
 */
function sendResult(&$result) {
	header('Content-type: application/json; charset=utf-8');
	header('Access-Control-Allow-Origin: *');		
	$jsonData = json_encode($result);
	echo $jsonData;
}