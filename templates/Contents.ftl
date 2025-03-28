<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
    <div class="screenlet-body">
      <form id="uploadPartyContent" method="post" enctype="multipart/form-data" action="<@ofbizUrl>${target}</@ofbizUrl>">
 <#if (order)?has_content><input type="hidden" name="orderId" value="${order.orderId}"/></#if>
  <#if (shipment)?has_content><input type="hidden" name="shipmentId" value="${shipment.shipmentId}"/></#if>
        <input type="file" name="uploadFile" class="required error" size="25"/>
       <#-- <div>
      <div class="label">${uiLabelMap.orderItemRowsNumber}</div>
      </div>
        <div>
        <input type="text" name="rows" size="4" maxlength="4"/>
        </div>-->
        <div>
        <input type="submit" value="${uiLabelMap.CommonUpload}" />
        </div>
      </form>
      </div>
  <script type="application/javascript">
    jQuery("#uploadPartyContent").validate({
        submitHandler: function(form) {
            <#-- call upload scripts - functions defined in PartyProfileContent.js -->
            uploadPartyContent();
            getUploadProgressStatus();
            form.submit();
        }
    });
  </script>
