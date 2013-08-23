/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

IMPORT 'macros.pig';

a1 = loadResources('$LOG', '$FROM_DATE', '$TO_DATE', '$USER', '$WS');
a3 = filterByEvent(a1, '$EVENT');
a5 = extractParam(a3, '$PARAM', 'param');
a6 = FOREACH a5 GENERATE ws, param;
a = FILTER a6 BY ws != 'default';

b1 = GROUP a BY (param, ws);
result = FOREACH b1 GENERATE TOBAG(group.param, group.ws), COUNT(a);


