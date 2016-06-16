/*
Copyright 2009-2015 Igor Polevoy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.javalite.activejdbc;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.javalite.activejdbc.test.ActiveJDBCTest;
import org.javalite.activejdbc.test_models.*;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.javalite.activejdbc.test_models.Person.Person;
import static org.javalite.activejdbc.test_models.User.User;
import static org.javalite.activejdbc.test_models.Article.Article;
import static org.javalite.activejdbc.test_models.Comment.Comment;
import static org.javalite.activejdbc.test_models.Tag.Tag;
import static org.javalite.activejdbc.test_models.Post.Post;



/**
 * @author Igor Polevoy
 * @author Eric Nielsen
 */
public class ToJsonSpec extends ActiveJDBCTest {
    @Test
    public void shouldGenerateSimpleJson() {
        deleteAndPopulateTable("people");
        Person p = Person.findById(1);
        //test no indent
        String json = p.toJson(false, "name", "last_name", "dob");
        Map  map = JsonHelper.toMap(json);

        a(map.get("name")).shouldBeEqual("John");
        a(map.get("last_name")).shouldBeEqual("Smith");
        a(map.get("dob")).shouldBeEqual("1934-12-01");
    }

    @Test
    public void shouldIncludePrettyChildren() {
        deleteAndPopulateTables("users", "addresses");
        List<User> personList = User.findAll().orderBy("id").include(Address.class);
        User u = personList.get(0);
        String json = u.toJson(true);

        Map m = JsonHelper.toMap(json);

        a(m.get("first_name")).shouldBeEqual("Marilyn");
        a(m.get("last_name")).shouldBeEqual("Monroe");

        Map children = (Map) m.get("children");
        List<Map> addresses = (List<Map>) children.get("addresses");

        the(addresses.size()).shouldBeEqual(3);
        //at this point, no need to verify, since the order of addresses is not guaranteed.. or is it??
    }

    @Test
    public void shouldIncludeUglyChildren() {
        deleteAndPopulateTables("users", "addresses");
        List<User> personList = User.findAll().orderBy("id").include(Address.class);
        User u = personList.get(0);
        String json = u.toJson(false);
        Map m = JsonHelper.toMap(json);

        a(m.get("first_name")).shouldBeEqual("Marilyn");
        a(m.get("last_name")).shouldBeEqual("Monroe");

        Map children = (Map) m.get("children");
        List<Map> addresses = (List<Map>) children.get("addresses");

        the(addresses.size()).shouldBeEqual(3);
        //at this point, no need to verify, since the order of addresses is not guaranteed.. or is it??
    }

    @Test
    public void shouldIncludeOnlyProvidedAttributes() {
        deleteAndPopulateTables("users", "addresses");

        User u = User.findById(1);
        String json = u.toJson(true, "email", "last_name");
        JsonHelper.readTree(json); // validate
        the(json).shouldBeEqual("{\n" +
                "  \"email\":\"mmonroe@yahoo.com\",\n" +
                "  \"last_name\":\"Monroe\"\n" +
                "}");
    }

    @Test
    public void shouldGenerateFromList() {
        deleteAndPopulateTables("users", "addresses");
        LazyList<User> personList = User.findAll().orderBy("id").include(Address.class);

        String json = personList.toJson(false);
        JsonHelper.readTree(json); // validate
    }

    @Test
    public void shouldEscapeDoubleQuote() {
        Page p = new Page();
        p.set("description", "bad \"/description\"");
        JsonNode node = JsonHelper.readTree(p.toJson(true));
        a(node.get("description").toString()).shouldBeEqual("\"bad \\\"/description\\\"\"");

        //ensure no NPE:
        p = new Page();
        p.set("description", null);
        p.toJson(true);
    }


    @Test
    public void shouldInjectCustomContentIntoJson() {
        deleteAndPopulateTable("posts");

        Post p = Post.findById(1);
        String json = p.toJson(true, "title");

        Map map = JsonHelper.toMap(json);
        Map injected = (Map) map.get("injected");
        a(injected.get("secret_name")).shouldBeEqual("Secret Name");
    }

    @Test
    public void shouldReturnSecondsInDateTime() throws ParseException {
        Person p = new Person();
        p.set("name", "john", "last_name", "doe").saveIt();
        p.refresh();
        String json = p.toJson(true);

        System.out.println(json);
        @SuppressWarnings("unchecked")
        Map<String, String> map = JsonHelper.toMap(json);

        Date d = new ISO8601DateFormat().parse(map.get("created_at"));
        // difference between date in Json and in original model instance should be less than 1000 milliseconds
        a(Math.abs(d.getTime() - p.getTimestamp("created_at").getTime()) < 1000L).shouldBeTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGenerateJsonForPolymorphicChildren() {
        deleteAndPopulateTables("articles", "comments", "tags");
        Article a = Article.findFirst("title = ?", "ActiveJDBC polymorphic associations");
        a.add(Comment.create("author", "igor", "content", "this is just a test comment text"));
        a.add(Tag.create("content", "orm"));
        LazyList<Article> articles = Article.where("title = ?", "ActiveJDBC polymorphic associations").include(Tag.class, Comment.class);

        Map[] maps = JsonHelper.toMaps(articles.toJson(true));

        the(maps.length).shouldBeEqual(1);
        Map article = maps[0];
        List<Map> comments = (List<Map>) ((Map)article.get("children")).get("comments");
        List<Map> tags = (List<Map>) ((Map)article.get("children")).get("tags");

        the(comments.size()).shouldBeEqual(1);
        the(comments.get(0).get("content")).shouldBeEqual("this is just a test comment text");
        the(tags.size()).shouldBeEqual(1);
        the(tags.get(0).get("content")).shouldBeEqual("orm");
    }

    @Test
    public void shouldKeepParametersCase() {
        Person p = Person.create("name", "Joe", "last_name", "Schmoe");

        Map map = JsonHelper.toMap(p.toJson(true));
        a(map.get("name")).shouldBeEqual("Joe");
        a(map.get("last_name")).shouldBeEqual("Schmoe");

        map = JsonHelper.toMap(p.toJson(true, "Name", "Last_Name"));
        a(map.get("Name")).shouldBeEqual("Joe");
        a(map.get("Last_Name")).shouldBeEqual("Schmoe");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldIncludeParents() {
        deleteAndPopulateTables("libraries", "books", "readers");
        List<Book> books = Book.findAll().orderBy(Book.getMetaModel().getIdName()).include(Reader.class, Library.class);

        Map book = JsonHelper.toMap(books.get(0).toJson(true));
        Map parents = (Map) book.get("parents");
        the(parents.size()).shouldBeEqual(1);

        List<Map> libraries = (List<Map>) parents.get("libraries");
        the(libraries.size()).shouldBeEqual(1);

        Map library = libraries.get(0);
        a(library.get("address")).shouldBeEqual("124 Pine Street");
    }
}

