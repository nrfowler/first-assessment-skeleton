import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let defaultCmd

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [url]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    let host=args.url ? args.url : 'localhost'
    server = connect({ host: host, port: 8080 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let message=Message.fromJSON(buffer)
      if(message.getCommand()=="broadcast"){
        this.log(cli.chalk['bgRed'](message.toString()))
      }
      else if(message.getCommand()=="users"){
          this.log(cli.chalk['blue'](message.toString()))
        }
      else if(message.getCommand()=="connect"||message.getCommand()=="disconnect"){
          username=message.getUsername();
          this.log(cli.chalk['gray'](message.toString()))
        }
      else if(message.getCommand()&&message.getCommand().charAt(0)=="@"){
          this.log(cli.chalk['cyan']['italic'](message.toString()))

        }
      else if(message.getCommand()=="echo"){
          this.log(cli.chalk['bgBlack'](message.toString()))
        }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    let [ command, ...rest ] = words(input, /[^\s]+/g)
    let contents = rest.join(' ')

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
      defaultCmd=command
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defaultCmd=command
    } else if (command === 'users') {
      server.write(new Message({ username, command }).toJSON() + '\n')
      server.write(new Message({ username, command }).toJSON() + '\n')

      defaultCmd=command
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defaultCmd=command
    } else if (command.charAt(0)==='@') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defaultCmd='@'
    } else if (defaultCmd===undefined){
      this.log(`Command <${command}> was not recognized`)
    }
    else{
      this.log(`Running Default command <${defaultCmd}> `)
      contents=[command,contents].join(' ')
      command=defaultCmd
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }

    callback()
  })
