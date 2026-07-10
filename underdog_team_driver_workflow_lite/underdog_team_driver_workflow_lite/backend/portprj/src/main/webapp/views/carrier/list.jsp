<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="aaa.carrier_p.model.CarrierDTO" %>
<%
    List<CarrierDTO> carrierList = (List<CarrierDTO>) request.getAttribute("carrierList");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>운송사 목록</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { margin-bottom: 20px; }
        form { margin-bottom: 24px; padding: 16px; border: 1px solid #ddd; width: 720px; }
        input, select, button { padding: 8px; margin: 4px; }
        table { border-collapse: collapse; width: 900px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #f3f5f7; }
    </style>
</head>
<body>
<h1>운송사 목록</h1>

<form action="/carrier/reg" method="post">
    <input type="text" name="carrierName" placeholder="운송사명" required>
    <input type="text" name="carrierContact" placeholder="연락처">
    <input type="text" name="managerName" placeholder="담당자명">
    <select name="carrierStatus">
        <option value="ACTIVE">ACTIVE</option>
        <option value="HOLD">HOLD</option>
        <option value="STOP">STOP</option>
    </select>
    <button type="submit">등록</button>
</form>

<table>
    <thead>
    <tr>
        <th>ID</th>
        <th>운송사명</th>
        <th>연락처</th>
        <th>담당자</th>
        <th>상태</th>
    </tr>
    </thead>
    <tbody>
    <%
        if (carrierList == null || carrierList.isEmpty()) {
    %>
    <tr>
        <td colspan="5">등록된 운송사가 없습니다.</td>
    </tr>
    <%
        } else {
            for (CarrierDTO carrier : carrierList) {
    %>
    <tr>
        <td><%= carrier.getCarrierId() %></td>
        <td><%= carrier.getCarrierName() %></td>
        <td><%= carrier.getCarrierContact() %></td>
        <td><%= carrier.getManagerName() %></td>
        <td><%= carrier.getCarrierStatus() %></td>
    </tr>
    <%
            }
        }
    %>
    </tbody>
</table>
</body>
</html>
