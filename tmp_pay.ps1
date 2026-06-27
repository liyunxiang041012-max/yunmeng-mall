$body = Get-Content 'E:\cloud\yunmeng-mall-main\yunmeng-mall-main\tmp_pay.json' -Raw
Invoke-RestMethod -Uri 'http://localhost:8084/order/status' -Method Put -Body $body -ContentType 'application/json'