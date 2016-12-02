package ab.explore.android_background_web_server;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Mikhael LOPEZ on 14/12/2015.
 */
public class AndroidWebServer extends NanoHTTPD {

    public AndroidWebServer(int port) {
        super(port);
    }

    public AndroidWebServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body>";
        Map<String, String> parms = session.getParms();
        if (parms.get("firstname") == null) {
//            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";

            msg += "<form action='?' method = 'get'>\n <p>First name: <input type='text' name='firstname'></p>\n <p>Last name: <input type='text' name='lastname'></p><br><br> <input type='submit' value='Submit'> </form>";
        } else {
            msg += "<p>Hello, " + parms.get("firstname") +" "+parms.get("lastname") + "!</p>";
        }
        return newFixedLengthResponse( msg + "</body></html>\n" );
    }
}
