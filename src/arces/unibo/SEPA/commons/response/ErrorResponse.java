/* This class represents an error response message
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * This class represents an error response to a request
 *
 *
 The JSON serialization looks like:

 {"error" : {"message" : "<Error message>"}}

 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 * */

package arces.unibo.SEPA.commons.response;

import com.google.gson.JsonPrimitive;

/**
 * This class represents a generic error. 
 * 
 * If applies, the use of HTTP status codes is suggested 
 * (RFC 2616, https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html)
 *
	400 Bad Request
	401 Unauthorized
	402 Payment Required
	403 Forbidden
	404 Not Found
	405 Method Not Allowed
	406 Not Acceptable
	407 Proxy Authentication Required
	408 Request Timeout
	409 Conflict
	410 Gone
	411 Length Required
	412 Precondition Failed
	413 Request Entity Too Large
	414 Request-URI Too Long
	415 Unsupported Media Type
	416 Requested Range Not Satisfiable
	417 Expectation Failed

	500 Internal Server Error
	501 Not Implemented
	502 Bad Gateway
	503 Service Unavailable
	504 Gateway Timeout
	505 HTTP Version Not Supported

 * The JSON serialization looks like:
 *
 * {
 * 		"body" : "Internal Server Error: SPARQL endpoint not found" , 
 * 		"code" : 500
 * }
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */
public class ErrorResponse extends Response {	
	public ErrorResponse(Integer token,String message,int code) {
		super(token);

		json.add("body", new JsonPrimitive(message));
		json.add("code", new JsonPrimitive(code));
	}
}
