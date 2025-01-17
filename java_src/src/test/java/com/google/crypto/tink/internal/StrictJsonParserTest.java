// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * Tests that {@link StrictJsonParser}.
 *
 * <p>We currently test it together with {@link com.google.gson.internal.Streams#parse(JsonReader)},
 * to show where they do the same and where they don't.
 */
@RunWith(Theories.class)
public final class StrictJsonParserTest {

  // TODO(b/241828611) Remove this once we fully migrated to StrictJsonParser.parse.
  // Streams.parse sometimes throws an IOException and sometimes an JsonParseException.
  private JsonElement normalParse(String input) throws IOException {
    try {
      JsonReader jsonReader = new JsonReader(new StringReader(input));
      jsonReader.setLenient(false);
      return Streams.parse(jsonReader);
    } catch (JsonParseException e) {
      throw new IOException(e);
    }
  }

  public static final class TestCase {
    // TODO(juerg): Make expected not a string but a JsonElement.
    public final String name;
    public final String input;
    public final JsonElement expected;

    public TestCase(String name, String input, JsonElement expected) {
      this.name = name;
      this.input = input;
      this.expected = expected;
    }

    public TestCase(String name, String input) {
      this.name = name;
      this.input = input;
      this.expected = null;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static JsonArray jsonArray(JsonElement... elements) {
    JsonArray output = new JsonArray();
    for (JsonElement element : elements) {
      output.add(element);
    }
    return output;
  }

  public static JsonObject jsonObject(String name, JsonElement value) {
    JsonObject output = new JsonObject();
    output.add(name, value);
    return output;
  }

  @DataPoints("testCasesSuccess")
  public static final TestCase[] TEST_CASES_SUCCESS = {
    new TestCase("string", "\"xyz\"", new JsonPrimitive("xyz")),
    new TestCase("number", "42", new JsonPrimitive(42)),
    new TestCase("negative_number", "-42", new JsonPrimitive(-42)),
    new TestCase("float", "42.42", new JsonPrimitive(42.42)),
    new TestCase("negative_float", "-42.42", new JsonPrimitive(-42.42)),
    new TestCase("true", "true", new JsonPrimitive(true)),
    new TestCase("false", "false", new JsonPrimitive(false)),
    new TestCase("null", "null", JsonNull.INSTANCE),
    new TestCase(
        "array", "[\"a\",\"b\"]", jsonArray(new JsonPrimitive("a"), new JsonPrimitive("b"))),
    new TestCase("map", "{\"a\":\"b\"}", jsonObject("a", new JsonPrimitive("b"))),
    new TestCase("empty_string", "\"\"", new JsonPrimitive("")),
    new TestCase("empty_array", "[]", new JsonArray()),
    new TestCase("array_with_newline", "[\n]", new JsonArray()),
    new TestCase("array_with_tab", "[\t]", new JsonArray()),
    new TestCase("empty_map", "{}", new JsonObject()),
    new TestCase("map_with_empty_key", "{\"\":\"a\"}", jsonObject("", new JsonPrimitive("a"))),
    new TestCase(
        "nested_arrays",
        "[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]",
        jsonArray(
            jsonArray(
                jsonArray(
                    jsonArray(
                        jsonArray(
                            jsonArray(
                                jsonArray(
                                    jsonArray(
                                        jsonArray(
                                            jsonArray(
                                                jsonArray(
                                                    jsonArray(
                                                        jsonArray(
                                                            jsonArray(
                                                                jsonArray(
                                                                    jsonArray())))))))))))))))),
    new TestCase(
        "nested_maps",
        "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{}}}}}}}}}}}",
        jsonObject(
            "a",
            jsonObject(
                "a",
                jsonObject(
                    "a",
                    jsonObject(
                        "a",
                        jsonObject(
                            "a",
                            jsonObject(
                                "a",
                                jsonObject(
                                    "a",
                                    jsonObject(
                                        "a",
                                        jsonObject("a", jsonObject("a", new JsonObject()))))))))))),
    new TestCase("tRuE", "tRuE", new JsonPrimitive(true)),
    new TestCase("fAlSe", "fAlSe", new JsonPrimitive(false)),
    new TestCase("nUlL", "nUlL", JsonNull.INSTANCE),
    new TestCase(
        "mixed_array",
        "[\"a\", null, 1, 0.1, true, {\"a\":0}, [4]]",
        jsonArray(
            new JsonPrimitive("a"),
            JsonNull.INSTANCE,
            new JsonPrimitive(1),
            new JsonPrimitive(0.1),
            new JsonPrimitive(true),
            jsonObject("a", new JsonPrimitive(0)),
            jsonArray(new JsonPrimitive(4)))),
    new TestCase("tailing_newline", "\"a\"\n", new JsonPrimitive("a")),
    new TestCase(
        "whitespace", " { \"a\"\n: \n\"b\" \n } \n ", jsonObject("a", new JsonPrimitive("b"))),
    new TestCase("string_with_comment", "\"a/*b*/c\"", new JsonPrimitive("a/*b*/c")),
    new TestCase("string_with_excaped_unicode", "\"\\uA66D\"", new JsonPrimitive("ꙭ")),
    new TestCase("valid_utf16", "\"\\uD83D\\uDC69\"", new JsonPrimitive("👩")),
    new TestCase("valid_UTF8_1", "\"\\u002c\"", new JsonPrimitive(",")),
    new TestCase("valid_UTF8_3", "\"\\u0123\"", new JsonPrimitive("ģ")),
    new TestCase("escapes", "\"\\\"\\\\\\/\\b\\f\\n\\r\\t\"", new JsonPrimitive("\"\\/\b\f\n\r\t")),
    new TestCase("newline", "\"a\\u000Ab\"", new JsonPrimitive("a\nb")),
    new TestCase("backslash", "\"\\u005C\"", new JsonPrimitive("\\")),
    new TestCase("double_quote", "\"\\u0022\"", new JsonPrimitive("\"")),
    new TestCase(
        "escaped_double_quote_in_key",
        "{\"\\\"\\\"\": 42}",
        jsonObject("\"\"", new JsonPrimitive(42))),
    new TestCase("escaped_null", "\"\\u0000\"", new JsonPrimitive("" + (char) 0x00)),
    new TestCase(
        "escaped_null_in_key",
        "{\"a\\u0000b\": 42}",
        jsonObject("a\u0000b", new JsonPrimitive(42))),
    new TestCase("invalid_UTF8", "\"日ш\"", new JsonPrimitive("日ш")),

    new TestCase("long_max_value", "9223372036854775807", new JsonPrimitive(9223372036854775807L)),
    new TestCase("big_float", "60911552482230981.0", new JsonPrimitive(6.0911552482230984e16)),
    new TestCase("exp", "4e+42", new JsonPrimitive(4e+42)),
    new TestCase("exp2", "4e42", new JsonPrimitive(4e+42)),
    new TestCase("Exp", "4E42", new JsonPrimitive(4e+42)),
    new TestCase("-exp", "-4e-42", new JsonPrimitive(-4e-42)),
    new TestCase("number_tailing_space", "42 ", new JsonPrimitive(42)),
    new TestCase("number_tailing_newline", "42\n", new JsonPrimitive(42)),
    new TestCase("number_tailing_formfeed", "42\f", new JsonPrimitive(42)),
    new TestCase(
        "float_close_to_zero",
        "0.000000000000000000000000000000001",
        new JsonPrimitive(0.000000000000000000000000000000001)),
    new TestCase(
        "-float_close_to_zero",
        "-0.000000000000000000000000000000001",
        new JsonPrimitive(-0.000000000000000000000000000000001)),
    new TestCase(
        "huge_number",
        "999999999999999999999999999999999999999999999999999999999999999999999999999999999999999",
        new JsonPrimitive(1e87)),
    new TestCase(
        "-huge_number",
        "-999999999999999999999999999999999999999999999999999999999999999999999999999999999999999",
        new JsonPrimitive(-1e87)),

    // For these two numbers, we couldn't find non-parsed numbers that are equal to them.
    new TestCase(
        "huge_exp",
        "99999999999999999999999999.99e+99999999999999999999999999",
        null),
    new TestCase(
        "-huge_exp",
        "-99999999999999999999999999.99e-99999999999999999999999999",
        null),

    new TestCase("string_with_tailing_comma", "\"a\",", new JsonPrimitive("a")),
    new TestCase("number_with_tailing_comma", "42,", new JsonPrimitive(42)),
    new TestCase("true_with_tailing_comma", "true,", new JsonPrimitive(true)),
    new TestCase("string_with_tailing_comment", "\"a\"/*comment*/", new JsonPrimitive("a")),
    new TestCase("map_with_tailing_comma", "{\"a\":1},", jsonObject("a", new JsonPrimitive(1))),
    new TestCase(
        "map_with_tailing_comment", "{\"a\":1}/*comment*/", jsonObject("a", new JsonPrimitive(1))),
    new TestCase(
        "map_with_tailing_open_comment",
        "{\"a\":1}/*comment",
        jsonObject("a", new JsonPrimitive(1))),
    new TestCase("map_with_tailing_#", "{\"a\":1}#", jsonObject("a", new JsonPrimitive(1))),
    new TestCase("map_with_tailing_]", "{\"a\":1}]", jsonObject("a", new JsonPrimitive(1))),
    new TestCase("map_with_tailing_}", "{\"a\":1}}", jsonObject("a", new JsonPrimitive(1))),
    new TestCase("array_with_tailing_comma", "[\"a\"],", jsonArray(new JsonPrimitive("a"))),
    new TestCase(
        "array_with_tailing_comment", "[\"a\"]/*comment*/", jsonArray(new JsonPrimitive("a"))),
    new TestCase(
        "array_with_tailing_open_comment", "[\"a\"]/*comment", jsonArray(new JsonPrimitive("a"))),
    new TestCase("array_with_tailing_#", "[\"a\"]#", jsonArray(new JsonPrimitive("a"))),
    new TestCase("array_with_tailing_]", "[\"a\"]]", jsonArray(new JsonPrimitive("a"))),
    new TestCase("array_with_tailing_}", "[\"a\"]}", jsonArray(new JsonPrimitive("a"))),
    new TestCase("double_array", "[][]", new JsonArray()),
    new TestCase("number_with_space", "42 000", new JsonPrimitive(42)),
    new TestCase("float_with_space", "42 000.0", new JsonPrimitive(42)),
  };

  @Theory
  public void testBothParsers_success(
      @FromDataPoints("testCasesSuccess") TestCase testCase) throws Exception {
    JsonElement strictOutput = StrictJsonParser.parse(testCase.input);

    // TODO(juerg): Remove this once invalid UTF16 strings are rejected.
    if (testCase.expected != null) {
      assertThat(strictOutput).isEqualTo(testCase.expected);
    }

    // compare to normalParse
    JsonElement normalOutput = normalParse(testCase.input);
    assertThat(strictOutput).isEqualTo(normalOutput);
  }

  @Theory
  public void testNumbersToLong() throws Exception {
    // After parsing, numbers are represented as LazilyParsedNumber, which contain the unparsed
    // string value of the number. When getAsLong() is called, then LazilyParsedNumber will convert
    // the string into a long. It first tries to parse it using Long.parseLong(value), and if that
    // fails, it tries new BigDecimal(value).longValue(), which will create a BigInteger and
    // call longValue() on that.
    assertThat(StrictJsonParser.parse("44444444444444444").getAsLong())
        .isEqualTo(44444444444444444L);
    // 2^63 - 1
    assertThat(StrictJsonParser.parse("9223372036854775807").getAsLong()
      ).isEqualTo(9223372036854775807L);

    // 2^63 + 1. Parsing as long fails. Parsing as BigDecimal works, but convertion to long wraps
    // around and becomes negative.
    assertThat(StrictJsonParser.parse("9223372036854775809").getAsLong()
      ).isEqualTo(-9223372036854775807L);
    // 2^64 + 42. BigInteger gives the number mod 2^64.
    assertThat(StrictJsonParser.parse("18446744073709551658").getAsLong()
      ).isEqualTo(42);
    // float, a bit smaller than 2^64. Parsing as long fails, parsing as BigDecimal
    // works, converting to long wraps around and results in a relatively small negative number.
    assertThat(StrictJsonParser.parse("1.8446744e+19").getAsLong()).isEqualTo(-73709551616L);
    // float, a bit larger than 2^64. Same as above, but now results in a small positive number.
    assertThat(StrictJsonParser.parse("1.8446745e+19").getAsLong()
      ).isEqualTo(926290448384L);
    assertThat(StrictJsonParser.parse("1e+63").getAsLong()
      ).isEqualTo(-9223372036854775808L);
    assertThat(
            StrictJsonParser.parse(
                    "999999999999999999999999999999999999999999999999999999999999999")  // 63 chars
                .getAsLong())
        .isEqualTo(9223372036854775807L);

    // Numbers with 64 or more decimal digits are not parsed. But the result seems to depends on
    // how many zeros the number has at the end.
    assertThat(StrictJsonParser.parse("1e+64").getAsLong()).isEqualTo(0);
    assertThat(
            StrictJsonParser.parse(
                    "9999999999999999999999999999999999999999999999999999999999999999")  // 64 chars
                .getAsLong())
        .isEqualTo(-1);
    assertThat(
            StrictJsonParser.parse(
                    "-9999999999999999999999999999999999999999999999999999999999999999")
                .getAsLong())
        .isEqualTo(1);
    assertThat(
            StrictJsonParser.parse(
                    "9999999999999999999999999999999999999999999999999999999999999000")
                .getAsLong())
        .isEqualTo(-1000);
    assertThat(
            StrictJsonParser.parse(
                    "9999999999999999999999999999999999999999999999999990000000000000")
                .getAsLong())
        .isEqualTo(-10000000000000L);
    assertThat(
            StrictJsonParser.parse(
                    "9999999999999999999999999999999999999999900000000000000000000000")
                .getAsLong())
        .isEqualTo(-200376420520689664L);
  }

  @Theory
  public void testNumbersToDouble() throws Exception {
    assertThat(StrictJsonParser.parse("60911552482230981.0").getAsDouble()
      ).isEqualTo(6.0911552482230984e16);
    assertThat(StrictJsonParser.parse("4e+42").getAsDouble()
      ).isEqualTo(4e42);
    assertThat(StrictJsonParser.parse("4e42").getAsDouble()
      ).isEqualTo(4e42);
    assertThat(StrictJsonParser.parse("4E42").getAsDouble()
      ).isEqualTo(4e42);
    assertThat(StrictJsonParser.parse("-4e-42").getAsDouble()
      ).isEqualTo(-4e-42);
    assertThat(
            StrictJsonParser.parse(
                    "9999999999999999999999999999999999999999999999999999999999999999999999999999"
                        + "99999999999")
                .getAsDouble())
        .isEqualTo(1.0e87);
    assertThat(
            StrictJsonParser.parse(
                    "-999999999999999999999999999999999999999999999999999999999999999999999999999"
                        + "999999999999")
                .getAsDouble())
        .isEqualTo(-1.0e87);
    assertThat(
            StrictJsonParser.parse("99999999999999999999999999.99e+99999999999999999999999999")
                .getAsDouble())
        .isPositiveInfinity();
    assertThat(
            StrictJsonParser.parse("-99999999999999999999999999.99e+99999999999999999999999999")
                .getAsDouble())
        .isNegativeInfinity();
    assertThat(
            StrictJsonParser.parse("99999999999999999999999999.99e-99999999999999999999999999")
                .getAsDouble())
        .isEqualTo(0.0);
    assertThat(StrictJsonParser.parse("0.000000000000000000000000000000001").getAsDouble()
      ).isEqualTo(0.000000000000000000000000000000001);
    assertThat(StrictJsonParser.parse("-0.000000000000000000000000000000001").getAsDouble()
      ).isEqualTo(-0.000000000000000000000000000000001);
    assertThat(StrictJsonParser.parse("42").getAsInt()
      ).isEqualTo(42);
    assertThat(StrictJsonParser.parse("42\n").getAsInt()
      ).isEqualTo(42);
    assertThat(StrictJsonParser.parse("42\f").getAsInt()
      ).isEqualTo(42);
  }

  @DataPoints("testCasesFail")
  public static final TestCase[] TEST_CASES_FAIL = {
    new TestCase("nested_empty_maps", "{{}}"),
    new TestCase("open_map", "{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":"),
    new TestCase("open_array_map", "[{\"\":[{\"\":[{\"\":[{\"\":[{\"\":[{\"\":[{\"\":["),
    new TestCase("open_array", "["),
    new TestCase("open_array_1", "[1"),
    new TestCase("open_array_2", "[1,"),
    new TestCase("open_arrays", "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[["),
    new TestCase("open_array_with_huge_negative_int", "[-2374623746732768942798327498324234"),
    new TestCase("map_missing_value", "{\"a\":"),
    new TestCase("string_not_closed", "\"a"),
    new TestCase("empty_string_not_closed", "\""),
    new TestCase("string_with_backslash_not_closed", "\"\\"),
    new TestCase("number_dot", "42."),
    new TestCase("-number_dot", "-42."),
    new TestCase("number_dot_with_e1", "42.e1"),
    new TestCase("number_dot_with_+e1", "42.e+1"),
    new TestCase("number_dot_with_-e1", "42.e-1"),
    new TestCase("number_with_e", "42e"),
    new TestCase("number_with_e+", "42e+"),
    new TestCase("number_with_E", "42E"),
    new TestCase("number_with_E+", "42E+"),
    new TestCase("float_with_e", "42.42e"),
    new TestCase("float_with_e+", "42.42e+"),
    new TestCase("float_with_E", "42.42E"),
    new TestCase("float_with_E+", "42.42E+"),
    new TestCase("+number", "+42"),
    new TestCase("++number", "++42"),
    new TestCase("Inf", "Inf"),
    new TestCase("+Inf", "+Inf"),
    new TestCase("-Inf", "-Inf"),
    new TestCase("Infinity", "Infinity"),
    new TestCase("+Infinity", "+Infinity"),
    new TestCase("-Infinity", "-Infinity"),
    new TestCase("NaN", "NaN"),
    new TestCase("+NaN", "+NaN"),
    new TestCase("-NaN", "-NaN"),
    new TestCase("number_dot_minus_number", "42.-42"),
    new TestCase("dot_minus_number", ".-42"),
    new TestCase("dot_number", ".42"),
    new TestCase("minus_dot_number", "-.42"),
    new TestCase("number_with_leading_zero", "042"),
    new TestCase("-number_with_leading_zero", "-042"),
    new TestCase("number_two_dots", "42.43.44"),
    new TestCase("number_two_dots_2", ".42.43"),
    new TestCase("number_two_dots_3", "42.43."),
    new TestCase("number_ee", "42ee42"),
    new TestCase("number_eE", "42eE42"),
    new TestCase("number_e_plus_minus", "42e+-42"),
    new TestCase("number_with_trailing_garbage", "2@"),
    new TestCase("number_with_tailing_comment", "42/*comment*/"),
    new TestCase("number_garbage_after_e", "1ea"),
    new TestCase("number_with_a", "1.2a-3"),
    new TestCase("number_with_h", "1.8011670033376514H-308"),
    new TestCase("hex1", "0x1"),
    new TestCase("hex2", "0x42"),
    new TestCase("number_with_tailing_a", "42a"),
    new TestCase("float_with_tailing_a", "42.42a"),
    new TestCase("minus_number_with_tailing_a", "-42a"),
    new TestCase("number_tailing_excaped_newline", "42\\n"),
    new TestCase("minus_a", "-a"),
    new TestCase("minus", "-"),
    new TestCase("addition", "1+2"),
    new TestCase("subtraction", "2-1"),
    new TestCase("multiplication", "2*1"),
    new TestCase("array_with_number_with_space", "[1 000]"),
    new TestCase("array_with_float_with_space", "[1 000.0]"),
    new TestCase("array_with_minus_space_number", "[- 42]"),
    new TestCase("key_without_quotes", "{a:0}"),
    new TestCase("key_single_quote", "{'a':0}"),
    new TestCase("array_element_without_quotes", "[a,0]"),
    new TestCase("array_element_single_quotes", "['a',0]"),
    new TestCase("map_with_trailing_comma", "{\"a\":0,}"),
    new TestCase("map_with_two_commas", "{\"a\":0,,\"b\":1}"),
    new TestCase("array_with_trailing_comma", "[\"a\",]"),
    new TestCase("map_with_comment", "{\"a\":/*comment*/\"b\"}"),
    new TestCase("map_with_null_key", "{null:0}"),
    new TestCase("map_with_number_key", "{1:1}"),
    new TestCase("map_with_huge_float_key", "{9999E9999:1}"),
    new TestCase("map_missing_colon", "{\"a\" \"b\"}"),
    new TestCase("map_missing_key", "{:\"b\"}"),
    new TestCase("map_with_comma", "{\"a\", \"b\"}"),
    new TestCase("map_double_colon", "{\"x\"::\"b\"}"),
    new TestCase("map_with_garbage", "{\"a\":\"b\" c}"),
    new TestCase("map_with_single_string", "{ \"a\" : \"b\", \"c\" }"),
    new TestCase("array_leading_comma", "[,1]"),
    new TestCase("array_double_comma", "[1,,2]"),
    new TestCase("array_double_tailing_comma", "[1,,]"),
    new TestCase("array_comma", "[,]"),
    new TestCase("nested_arrays_no_comma", "[3[4]]"),
    new TestCase("array_without_comma", "[1 2]"),
    new TestCase("array__with_colon", "[\"a\": 1]"),
    new TestCase("incomplete_false", "fals"),
    new TestCase("incomplete_null", "nul"),
    new TestCase("incomplete_true", "tru"),
    new TestCase("unquoted_string", "a"),
    new TestCase("star", "*"),
    new TestCase("angle_bracket_dot", "<.>"),
    new TestCase("string_escape_x", "\"\\x00\""),
    new TestCase("escaped_emoji", "\"\\👩\""),
    new TestCase("invalid_backslash_escape", "\"\\a\""),
    new TestCase("unicode_with_capital_u", "\"\\UA66D\""),
    new TestCase("invalid_unicode_escape", "\"\\uqqqq\""),
    new TestCase("incomplete_surrogate", "\"\\uD834\\uDd\""),
    new TestCase("1_surrogate_then_escape_u", "[\"\\uD800\\u\"]"),
    new TestCase("2_incomplete_surrogate_escape_invalid", "[\"\\uD800\\uD800\\x\"]"),
    new TestCase("array_with_formfeed", "[\f]"),
    new TestCase("array_with_tailing_formfeed", "[\"a\"\f]"),
    new TestCase("array_with_leading_uescaped_thinspace", "[\\u0020\"a\"]"),
    new TestCase("array_with_escaped_new_line", "[\\n]"),
    new TestCase("array_with_escaped_tab", "[\\t]"),
  };

  @Theory
  public void testBothParsersFail(
      @FromDataPoints("testCasesFail") TestCase testCase) throws Exception {
    assertThrows(IOException.class, () -> StrictJsonParser.parse(testCase.input));
    assertThrows(IOException.class, () -> normalParse(testCase.input));
  }

  @DataPoints("stricterTestCases")
  public static final TestCase[] STRICTER_TEST_CASES = {
    new TestCase("duplicated_key", "{\"a\":\"b\",\"a\":\"c\"}"),
    new TestCase("duplicated_key_and_value", "{\"a\":\"b\",\"a\":\"b\"}"),
    new TestCase("empty", ""),
    new TestCase("single_space", " "),
    new TestCase("nested_with_duplicated_key", "{\"x\":{\"a\":\"b\",\"a\":\"c\"}}"),
    new TestCase("split_array", "{ \"a\" : [1,2,3], \"b\" : 0, \"a\" : [4,5,6]}"),

    // invalid characters in strings
    new TestCase("invalid_utf16", "\"\\uD834\"", null),
    new TestCase("invalid_utf16_in_key", "{\"\\ud800\\ud800key\":\"value\"}", null),
    new TestCase(
         "invalid_utf16_in_key_2", "{\"key\":\"value1\",\"\\ud800\\ud800key\":\"value2\"}", null),
    new TestCase("invalid_utf16_in_value", "{\"key\":\"value\\ud800\\ud800\"}", null),
    new TestCase("invalid_surrogate_1", "\"\\uDADA\"", null),
    new TestCase("invalid_surrogate_2", "\"\\ud800\"", null),
    new TestCase("invalid_surrogate_3", "\"\\uDd1ea\"", null),
    new TestCase("invalid_surrogate_4", "\"\\uDFAA\"", null),
    new TestCase("invalid_surrogate_5", "\"\\uD888\\u1234\"", null),
    new TestCase("invalid_surrogate_6", "\"\\uD800\\uD800\\n\"", null),
    new TestCase("invalid_surrogate_7", "\"\\uDd1e\\uD834\"", null),
    new TestCase("invalid_surrogate_in_map_key", "{\"\\uDFAA\":0}", null),
    new TestCase("invalid_surrogate_in_map_value", "{\"a\": \"\\uDFAA\"}", null),
  };

  @Theory
  public void testStrictFailsButNormalDoesNotFail(
      @FromDataPoints("stricterTestCases") TestCase testCase) throws Exception {
    // StrictJsonParser.parse fails.
    assertThrows(IOException.class, () -> StrictJsonParser.parse(testCase.input));

    // normalParse parses successfully.
    JsonElement unused = normalParse(testCase.input);
  }
}
