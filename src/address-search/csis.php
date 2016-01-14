<?php
/**
 * 東京大学CSIS シンプルジオコーディング実験サービス　地理院地図連携　サンプルコード
 */
abstract class CSISDataSource {
	private $csisUrl = 'http://geocode.csis.u-tokyo.ac.jp/cgi-bin/simple_geocode.cgi';
	private $dataType;
	function __construct($dataType) {
		$this->dataType = $dataType;
	}
	function search($searchWord) {
		$url = $this->csisUrl . "?charset=UTF8&geosys=world&series=" . $this->dataType . "&addr=" . urlencode($searchWord);
		$xmlResult = @file_get_contents($url);
		if ($xmlResult === FALSE) {
			return FALSE;
		}
		$parsedData = simplexml_load_string($xmlResult);
		$records = array();
		if (isset($parsedData->candidate)) {
			$candidate = $parsedData->candidate;
			if (count($candidate) > 0) {
				for ($i = 0; $i < count($candidate); $i++) {
					$row = $candidate[$i];
					if (isset($row->address) && isset($row->iLvl) && $row->iLvl > 0) {
						$records[] = $this->parseRow($row);
					}					
				}
			} else {
				if (isset($candidate->address)) {
					$records[] = $this->parseRow($candidate);
				}
			}
		}
		return $records;
	}
	abstract function parseRow($row);
}

class CSISAddressDataSource  extends CSISDataSource {
	function __construct() {
		parent::__construct("ADDRESS");
	}
	function parseRow($row) {
		$tokens = explode("/",$row->address);
		$pref = "";
		$muniNm = "";
		$address = "";
		for ($i = 0; $i < count($tokens); $i++) {
			if ($i == 0) {
				$pref = $tokens[$i];
			}
			if (substr($tokens[$i],-1) == "郡") {
				continue;
			} else if (substr($tokens[$i],-1) == "区" && substr($tokens[$i-1],-1) == "市") {
				$muniNm = $tokens[$i-1] . $tokens[$i];
			}
			$address = $address . $tokens[$i];
			
		}
		$addressCode = "";
		$record = array(
			"type" => "Feature",
			"geometry" => array(
				"type" => "Point",
				"coordinates" => array((float)$row->longitude,(float)$row->latitude)
			),
			"properties" => array(
				"title" => $address,
				"addressCode" => $addressCode
			)
		);
		return $record;
	}
}
