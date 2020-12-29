package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlAvailableTest {
    @Test
    void testAvailable() {
        assertFalse(UrlAvailable.available("www.google.com"));
        assertTrue(UrlAvailable.available("http://www.google.com"));
        assertTrue(UrlAvailable.available("http://www.google.com:80"));
        assertFalse(UrlAvailable.available("http://www.bogusurllaila1234.com"));
    }

    @Test
    void testAnyAvailable() {
        List<String> l1 = List.of("http://www.google.com", "http://www.facebook.com");
        List<String> l2 = List.of("http://www.glajlasjdflkajsdfkljaskdfjlasjdfloogle.com", "http://www.facebook.com");
        List<String> l3 = List.of("http://www.google.com", "http://www.lkjasdkfljal;kdsjf;lakjsdflkjsdafacebook.com");
        List<String> l4 = List.of("http://www.goljaskdfj;ladjsfl;saogle.com", "http://www.fadsljflakjsdf;lasdfacebook.com");
        assertTrue(UrlAvailable.isAnyAvailable(l1));
        assertTrue(UrlAvailable.isAnyAvailable(l2));
        assertTrue(UrlAvailable.isAnyAvailable(l3));
        assertFalse(UrlAvailable.isAnyAvailable(l4));
    }
}