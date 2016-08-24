export class Message {
  static fromJSON (buffer) {
    let messages=[]
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents }) {
    this.username = username
    this.command = command
    this.contents = contents
  }
  setCommand(cmd){
    this.command=cmd
  }
  getCommand(){
    return this.command
  }
  setContents(con){
     this.contents=con
  }
  getUsername(){
    return this.username
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents
    })
  }

  toString () {
    return this.contents
  }
}
