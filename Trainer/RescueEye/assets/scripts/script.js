// @ Constants
const ACCEPTABLE_FORMATS = ["mp4", "MP4"];
const sessionIDKey = "sessionID";

// @ global variables
sessionID = null;

let uploadBtn = document.getElementById("upload");
let trainModelBtn = document.getElementById("train-model");
let downloadBtn = document.getElementById("download");
disableButton(uploadBtn, true);
disableButton(trainModelBtn, true);
disableButton(downloadBtn, false);

// @eyeBallHandler
document.querySelector("body").addEventListener("mousemove", eyeBallHandler);

function eyeBallHandler() {
  var eyes = document.querySelectorAll(".eye");
  eyes.forEach(eye => {
    let x = eye.getBoundingClientRect().left + eye.clientWidth / 2;
    let y = eye.getBoundingClientRect().top + eye.clientHeight / 2;
    let radian = Math.atan2(event.pageX - x, event.pageY - y);
    let rot = radian * (180 / Math.PI) * -1 + 270;
    eye.style.transform = "rotate(" + rot + "deg)";
  });
}

function setSessionId() {
  document.cookie.setItem(sessionIDKey, JSON.stringify(sessionID));
}

function setCookie() {
  cname = "sessionId";
  const sessionID = Math.random()
    .toString(26)
    .substring(6);
  document.cookie = cname + "=" + sessionID + ";" + ";path=/";
}

function getCookie(cname) {
  var name = cname + "=";
  var decodedCookie = decodeURIComponent(document.cookie);
  var ca = decodedCookie.split(";");
  for (var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == " ") {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}

function disableButton(btn, disable) {
  if (disable) {
    btn.style.pointerEvents = "none";
    btn.style.opacity = "0.4";
  } else {
    btn.style.pointerEvents = "unset";
    btn.style.opacity = "1";
  }
}

function getSessionId() {
  try {
    return document.cookie;
  } catch (error) {
    // notify failing parsing using toaster(phase 2)
  }
}

// @ validate input file
function validateFileType(input) {
  let fileName = input.value,
    idxDot = fileName.lastIndexOf(".") + 1,
    extFile = fileName.substr(idxDot, fileName.length);
  setCookie();
  console.log(uploadBtn);
  if (!ACCEPTABLE_FORMATS.includes(extFile)) {
    alert("Only mp4/MP4 files are allowed!");
    input.value = "";
    disableButton(uploadBtn, true);
  } else {
    disableButton(uploadBtn, false);
  }
}
