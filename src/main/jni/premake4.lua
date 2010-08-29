solution "jeff"
   configurations { "Debug", "Release" }
 
   -- A project defines one build target
   project "jeffbinary"
      kind "SharedLib"
      language "C"
      includedirs {"/Library/Java/Home/include"}
      files { "**.h", "**.c" }

      configuration "Debug"
         defines { "DEBUG" }
         flags { "Symbols" }
 
      configuration "Release"
         defines { "NDEBUG" }
         buildoptions { "-O3" }

   project "benchmark"
      kind "ConsoleApp"
      language "C"
      includedirs {"/Library/Java/Home/include"}
      files { "**.h", "**.c" }

      configuration "Debug"
         defines { "DEBUG" }
         flags { "Symbols" }
         linkoptions { "-lprofiler" }

      configuration "Release"
         defines { "NDEBUG" }
         flags { "Symbols" }
         buildoptions { "-O3" }
         linkoptions { "-lprofiler" }
