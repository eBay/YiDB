# python-cmsclient

Python client for cms including library and CLI

## Usage

With local copy of metadata :    
```
./cms --service cms.vip.stratus.ebay.com --repo cmsdb --metadata ../cmsclient/tests/resources/metadata.json obj find 'Asset{*}.asset!AssetServer[@resourceId="Serial-USE009Y01F"]'
```

without:    
```
./cms --service cms.vip.stratus.ebay.com --repo cmsdb obj find 'Asset{*}.asset!AssetServer[@resourceId="Serial-USE009Y01F"]'
./cms --service phx5qa01c-5fca.stratus.phx.qa.ebay.com:8080 --repo cmsdb obj create Region '{"resourceId" : "Region27",  "description" : "Region SLC change 2"}'
./cms --service phx5qa01c-5fca.stratus.phx.qa.ebay.com:8080 --repo cmsdb obj update Region 52d436c9e4b0ee66348b4518 '{"resourceId" : "Region27",  "description" : "Region SLC change 3"}'
./cms --service phx5qa01c-5fca.stratus.phx.qa.ebay.com:8080 --repo cmsdb obj delete Region 52d436c9e4b0ee66348b4518 
```

Passing environment variables
```
export CMS_SERVICE=cms.vip.stratus.ebay.com  
export CMS_REPO=cmsdb         
./cms find 'Asset{*}.asset!AssetServer[@resourceId="Serial-USE009Y01F"]'
```